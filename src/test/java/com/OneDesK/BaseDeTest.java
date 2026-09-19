package com.OneDesK;

import java.util.List;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Hace que todos los tests usen la base onedesk_test en lugar de la base real onedesk.
 *
 * Se registra en src/test/resources/META-INF/spring.factories y lo aplica el framework de tests de Spring
 * a cada contexto que arma para un test. La aplicacion nunca lo usa, aunque este en el classpath: por eso
 * se hace asi y no con un application.properties de test. Eclipse, al correr App.java, tambien carga los
 * recursos de test, y un application.properties de test le cambiaba la base a la aplicacion real.
 */
public class BaseDeTest implements ContextCustomizerFactory {

	static final String URL = "jdbc:mysql://localhost:3306/onedesk_test";

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		return new UsarBaseDeTest();
	}

	/** Pisa la URL de la base antes de que se cree la conexion. */
	static class UsarBaseDeTest implements ContextCustomizer {

		@Override
		public void customizeContext(ConfigurableApplicationContext contexto, MergedContextConfiguration configuracion) {
			TestPropertyValues.of("spring.datasource.url=" + URL).applyTo(contexto);
		}

		// todos los tests piden lo mismo: asi Spring reutiliza el contexto entre clases de test
		@Override
		public boolean equals(Object otro) {
			return otro instanceof UsarBaseDeTest;
		}

		@Override
		public int hashCode() {
			return UsarBaseDeTest.class.hashCode();
		}
	}
}
