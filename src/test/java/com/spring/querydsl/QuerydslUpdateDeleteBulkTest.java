package com.spring.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spring.querydsl.entity.Member;
import com.spring.querydsl.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static com.spring.querydsl.entity.QMember.member;


@SpringBootTest
@Transactional
public class QuerydslUpdateDeleteBulkTest {
    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void update() throws Exception {
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                /** 기존 숫자에 더하기
                .set(member.age, member.age.add(1))
                    기존 숫자에 곱하기
                .set(member.age, member.age.multiply(2))
                 */
                .where(member.age.lt(28))
                .execute();

        Assertions.assertThat(count).isEqualTo(2);
    }

    @Test
    public void delete() throws Exception {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        Assertions.assertThat(count).isEqualTo(3);
    }
}




