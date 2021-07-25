package com.spring.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import com.spring.querydsl.entity.Member;
import com.spring.querydsl.entity.Team;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static com.spring.querydsl.entity.QMember.*;
import static com.spring.querydsl.entity.QTeam.*;


@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @PersistenceUnit
    EntityManagerFactory emf;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member1", 20, teamA);
        Member member3 = new Member("member1", 20, teamB);
        Member member4 = new Member("member1", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void search() {
        // com.querydsl.core.Tuple : 프로젝션(select 대상 지정) 대상이 둘 이상일 때 사용
        List<Tuple> result = queryFactory
                // select, from을 selectFrom으로 합칠 수 있다.
                // .selectFrom(member)
                .select(member.username,
                        // 집합 함수
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                // 검색 조건은 .and(), .or()를 메서드 체인으로 연결할 수 있다.
                // .where(member.username.eq("member1").and(member.age.gt(10)))
                // -> AND 조건을 파라미터로 처리하면 null 값은 무시한다.
                .where(member.username.eq("member1"), member.age.gt(10))
                /*
                member.username.eq("member1") // username = 'member1'
                member.username.ne("member1") //username != 'member1'
                member.username.eq("member1").not() // username != 'member1'
                member.username.isNotNull() //이름이 is not null
                member.age.in(10, 20) // age in (10,20)
                member.age.notIn(10, 20) // age not in (10, 20)
                member.age.between(10,30) //between 10, 30
                member.age.goe(30) // age >= 30
                member.age.gt(30) // age > 30
                member.age.loe(30) // age <= 30
                member.age.lt(30) // age < 30
                member.username.like("member%") //like 검색
                member.username.contains("member") // like ‘%member%’ 검색
                member.username.startsWith("member") //like ‘member%’ 검색
                */
                // Group by
                .groupBy(member.username, member.age)
                .having(member.age.gt(10))
                // Sort
                // 1. 회원 나이 내림차순(desc), 2. 회원 이름 올림차순(asc)
                // * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
                // nullsLast() , nullsFirst() : null 데이터 순서 부여
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                // Paging
                // .offset(1) //0부터 시작(zero index)
                // .limit(2) //최대 2건 조회
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }

    @Test
    public void join() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                // join(조인 대상, 별칭으로 사용할 Q타입)
                /**
                 * join() , innerJoin() : 내부 조인(inner join)
                 * leftJoin() : left 외부 조인(left outer join)
                 * rightJoin() : rigth 외부 조인(rigth outer join
                 */
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member1");
    }

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     * 외부조인(OUTER) 불가능
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 1. 조인 대상 필터링
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }

    @Test
    public void fetchJoin() throws Exception {
        em.flush();
        em.clear();

        /**
         * Memeber Entity의 Team
         *  @ManyToOne(fetch = FetchType.LAZY)
         *  @JoinColumn(name = "team_id")
         *  private Team team;
         *  > FetchType.LAZY(지연로딩)이기에 일반 조인할 경우 로딩하지 않는다.
         */
        Member result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(member.username.eq("member1"))
                .fetchFirst();

        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        System.out.println("loaded = " + loaded); // false

        /**
         * fetch join : SQL 조인을 활용해서 연관된 엔티티를 한번에 조회하는 기능 (즉시로딩)
         *  > 성능 최적화에 사용
         * 사용방법) join(), leftJoin() 등 조인 기능 뒤에 fetchJoin() 추가
         */
        Member fetchResult = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchFirst();

        System.out.println("findMember.getTeam() = " + fetchResult.getTeam());

        boolean fetchLoaded =
                emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        System.out.println("loaded = " + fetchLoaded); // true
    }

}