package com.spring.querydsl;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spring.querydsl.entity.Member;
import com.spring.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static com.spring.querydsl.entity.QMember.member;


@SpringBootTest
@Transactional
public class QuerydslFunctionTest {
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
    public void replace() throws Exception {
        String result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M"))
                .from(member)
                .fetchFirst();

        System.out.println("result = " + result); // M1
    }

    @Test
    public void lower() throws Exception {
        String result = queryFactory
                .select(member.username)
                .from(member)
                // .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
                /**
                 * ansi 표준 함수들은 querydsl이 상당부분 내장하고 있다.
                 */
                .where(member.username.eq(member.username.lower()))
                .fetchFirst();

        System.out.println("result = " + result); // member1
    }
}




