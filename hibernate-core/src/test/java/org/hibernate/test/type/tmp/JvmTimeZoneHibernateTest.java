/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.type.tmp;

import java.time.LocalDate;
import java.util.TimeZone;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import org.assertj.core.api.Assertions;
import org.h2.util.DateTimeUtils;

public class JvmTimeZoneHibernateTest extends BaseCoreFunctionalTestCase {

	private static final String ENTITY_NAME = "theentity";

	private final TimeZone timeZoneBefore;

	public JvmTimeZoneHibernateTest() {
		this.timeZoneBefore = TimeZone.getDefault();
	}

	@Override
	protected final Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Test
	public void test_rome() {
		withDefaultTimeZone( TimeZone.getTimeZone( "Europe/Rome" ), 1 );
	}

	@Test
	public void test_dallas() {
		withDefaultTimeZone( TimeZone.getTimeZone( "America/Dallas" ), 2 );
	}

	@After
	public void clearH2StaticCaches() {
		DateTimeUtils.resetCalendar();
	}

	private final void withDefaultTimeZone(TimeZone timeZone, int id) {
		TimeZone.setDefault( timeZone );

		try {
			test( id );
		}
		finally {
			TimeZone.setDefault( timeZoneBefore );
		}
	}

	private void test(int id) {
		MyEntity entity = new MyEntity( id, LocalDate.of( 1997, 5, 5 ) );
		inTransaction( session -> {
			session.persist( entity );
		} );

		// reloaded from another session / transaction
		inTransaction( session -> {
			MyEntity reloaded = session.find( MyEntity.class, id );
			Assertions.assertThat( reloaded.value ).isEqualTo( LocalDate.of( 1997, 5, 5 ) );
		} );
	}

	@Entity(name = ENTITY_NAME)
	private static final class MyEntity {

		@Id
		private Integer id;

		@Basic
		public LocalDate value;

		private MyEntity() {
		}

		public MyEntity(int id, LocalDate value) {
			this.id = id;
			this.value = value;
		}
	}
}
