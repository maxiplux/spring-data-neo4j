/*
 * Copyright 2011-2019 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.domain.sample.User;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.sample.UserRepository;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Integration tests for disabling default transactions using JavaConfig.
 *
 * @author Mark Angrish
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DefaultTransactionDisablingIntegrationIT extends MultiDriverTestClass {

	public @Rule ExpectedException exception = ExpectedException.none();

	@Autowired UserRepository repository;
	@Autowired TransactionalRepositoryIT.DelegatingTransactionManager txManager;

	/**
	 */
	@Test
	public void considersExplicitConfigurationOnRepositoryInterface() {

		repository.findOne(1L);

		assertThat(txManager.getDefinition().isReadOnly(), is(false));
	}

	/**
	 */
	@Test
	public void doesNotUseDefaultTransactionsOnNonRedeclaredMethod() {

		repository.findAll(new PageRequest(0, 10));

		assertThat(txManager.getDefinition(), is(nullValue()));
	}

	/**
	 */
	@Test
	public void persistingAnEntityShouldThrowExceptionDueToMissingTransaction() {

		exception.expect(InvalidDataAccessApiUsageException.class);
		exception.expectCause(is(Matchers.<Throwable> instanceOf(IllegalStateException.class)));

		repository.save(new User());
	}

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = UserRepository.class, enableDefaultTransactions = false)
	@EnableTransactionManagement
	static class Config {

		@Bean
		public TransactionalRepositoryIT.DelegatingTransactionManager transactionManager() throws Exception {
			return new TransactionalRepositoryIT.DelegatingTransactionManager(new Neo4jTransactionManager(sessionFactory()));
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory("org.springframework.data.neo4j.domain.sample");
		}
	}
}
