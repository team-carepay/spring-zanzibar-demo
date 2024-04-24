package com.carepay.demo.library;

import java.util.List;

import com.carepay.demo.BaseIntegrationTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(authorities = "admin")
class LibraryIntegrationTest extends BaseIntegrationTests {

    @Autowired
    HttpGraphQlTester httpGraphQlTester;

    @Test
    void testQueryAllBooks() {
        List<AuthorEntity> authors = httpGraphQlTester
                .document("""
                        query {
                        	authors {
                        		id
                        		firstName
                        		lastName
                        		user
                        	}
                        }
                        """)
                .execute()
                .errors()
                .verify()
                .path("authors")
                .entityList(AuthorEntity.class)
                .get();
        assertThat(authors).isNotEmpty();
        assertThat(authors).filteredOn(a -> "john".equals(a.firstName)).isNotEmpty();
    }
}
