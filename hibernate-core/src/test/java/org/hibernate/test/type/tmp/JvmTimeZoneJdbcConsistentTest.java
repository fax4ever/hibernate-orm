/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.type.tmp;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class JvmTimeZoneJdbcConsistentTest {

	private static final String DB_DRIVER = "org.h2.Driver";
	private static final String DB_CONNECTION = "jdbc:h2:~/test";
	private static final String DB_USER = "";
	private static final String DB_PASSWORD = "";

	private static final String DROP_TABLE_SQL = "drop table if exists theentity";
	private static final String CREATE_TABLE_SQL = "create table theentity (id integer not null, value date, primary key (id) )";
	private static final String INSERT_SQL = "insert into theentity (value, id) values (?, ?)";
	private static final String QUERY_SQL = "select changedefa0_.id as id1_0_0_, changedefa0_.value as value2_0_0_ from theentity changedefa0_ where changedefa0_.id=?";

	private final TimeZone timeZoneBefore;

	public JvmTimeZoneJdbcConsistentTest() {
		timeZoneBefore = TimeZone.getDefault();
	}

	@Test
	public void test_rome_than_dallas() {
		try {
			TimeZone.setDefault( TimeZone.getTimeZone( "Europe/Rome" ) );
			createTable();
			insertAndGetDate( 1 );
			dropTable();
		}
		finally {
			TimeZone.setDefault( timeZoneBefore );
		}
		try {
			TimeZone.setDefault( TimeZone.getTimeZone( "America/Dallas" ) );
			createTable();
			insertAndGetDate( 2 );
			dropTable();
		}
		finally {
			TimeZone.setDefault( timeZoneBefore );
		}
	}

	@After
	public void dropTable() {
		try (Connection connection = getConnection()) {
			connection.setAutoCommit( false );

			try (PreparedStatement preparedStatement = connection.prepareStatement( DROP_TABLE_SQL )) {
				preparedStatement.execute();
			}
		}
		catch (SQLException e) {
			Assertions.fail( e.getMessage(), e );
		}
	}

	private void createTable() {
		try (Connection connection = getConnection()) {
			connection.setAutoCommit( false );

			try (PreparedStatement preparedStatement = connection.prepareStatement( DROP_TABLE_SQL )) {
				preparedStatement.execute();
			}
			try (PreparedStatement preparedStatement = connection.prepareStatement( CREATE_TABLE_SQL )) {
				preparedStatement.execute();
			}
		}
		catch (SQLException e) {
			Assertions.fail( e.getMessage(), e );
		}
	}

	private void insertAndGetDate(int id) {
		try (Connection connection = getConnection()) {
			try (PreparedStatement preparedStatement = connection.prepareStatement( INSERT_SQL )) {
				Date sqlDate = Date.valueOf( LocalDate.of( 1997, 5, 5 ) );
				Assertions.assertThat( sqlDate ).hasDayOfMonth( 5 );

				preparedStatement.setDate( 1, sqlDate );
				preparedStatement.setInt( 2, id );
				preparedStatement.executeUpdate();
			}
			try (PreparedStatement preparedStatement = connection.prepareStatement( QUERY_SQL )) {
				preparedStatement.setInt( 1, id );
				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					Assertions.assertThat( resultSet ).isNotNull();
					Assertions.assertThat( resultSet.next() ).isTrue();
					Assertions.assertThat( resultSet.getDate( "value2_0_0_" ) ).hasDayOfMonth( 5 );
				}
			}
		}
		catch (SQLException e) {
			Assertions.fail( e.getMessage(), e );
		}
	}

	private static Connection getConnection() {
		Connection connection = null;
		try {
			Class.forName( DB_DRIVER );
		}
		catch (ClassNotFoundException e) {
			Assertions.fail( "Driver class not found. " + e.getMessage(), e );
		}
		try {
			return DriverManager.getConnection( DB_CONNECTION, DB_USER, DB_PASSWORD );
		}
		catch (SQLException e) {
			Assertions.fail( "Error acquiring connection. " + e.getMessage(), e );
		}
		return connection;
	}
}
