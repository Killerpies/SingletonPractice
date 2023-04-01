import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.truth.Truth;

//** LAZY ** //
class PCSEChairLazyTest {
	private static final String CLASS_NAME = "PCSEChairLazy";
	private static       boolean firstRead  = true;
	
	private Class<?> getClazz(String name) {
		Class<?> result = null;
		try {
			Package pkg  = getClass().getPackage();
			String  path = (pkg == null || pkg.getName().isEmpty()) ? "" : pkg.getName()+".";
			result = Class.forName( path + name );
		} catch (ClassNotFoundException e) {
			fail( String.format( "Class %s not found", name ));
		}
		return result;
	}
	private static void hasMethod(Class<?> clazz, Class<?> result, String name, Class<?>... args) {
		try {
			var method = clazz.getDeclaredMethod( name, args );

			Truth.assertWithMessage( String.format("'%s.%s' should be public", clazz.getSimpleName(), name ))
 				 .that( Modifier.isPublic( method.getModifiers() ))
				 .isTrue();

			Truth.assertWithMessage( String.format("'%s.%s' should return a value of type '%s'", clazz.getSimpleName(), name, result.getSimpleName() ))
				 .that( method.getReturnType() )
				 .isEqualTo( result );
		} catch (NoSuchMethodException | SecurityException e) {
			String params = Arrays.stream( args ).map( Class::getSimpleName ).collect( Collectors.joining( "," ));
			String msg    = String.format( "'%s' doesn't have method '%s %s(%s)'", clazz.getSimpleName(), result.getSimpleName(), name, params );
			fail( String.format( msg ));
		}					
	}
	private static final BiFunction<Class<?>,Predicate<Field>,List<Field>> getFields = (clazz,predicate) -> Arrays.stream ( clazz.getDeclaredFields())
             .filter ( f->!f.isSynthetic())
             .filter ( predicate )
             .collect( Collectors.toList());

	private static final BiFunction<Object,Field,?> getFieldValue = (object,field) -> {
		try {
			field.setAccessible( true );
			var value = field.get( object );
			return value;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	};	
	private static final BiFunction<Object,Field,Predicate<Object>> setFieldValue = (object,field) -> value -> {
		try {
			field.setAccessible( true );
			field.set( object, value );
			return true;
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return false;
	};
	private static final BiPredicate<Field,Object> setStaticFieldValue = (field, value) -> setFieldValue.apply( null, field ).test( value );
	private static final Function<Field,?> getStaticFieldValue = field -> getFieldValue.apply( null, field );

	@BeforeEach
	public void testInstanceFieldHasNullValue() {
		var              clazz     = getClazz( CLASS_NAME );
		Predicate<Field> areStatic = f -> Modifier.isStatic( f.getModifiers() );
		var              fields    = getFields.apply( clazz, areStatic );
		
		Truth.assertWithMessage( "only one static variable should exist" )
		     .that( fields )
		     .hasSize( 1 );

		var field = fields.get(0);
		var name  = field.getName();
		Truth.assertWithMessage( String.format( "static field '%s.%s' should not be final", CLASS_NAME, name ))
			 .that( Modifier.isFinal( field.getModifiers() ))
			 .isFalse();
	
		if (firstRead) {
			firstRead = false;
			var value = getStaticFieldValue.apply( field );
			Truth.assertWithMessage( String.format( "field '%s.%s' should be lazily initialized", CLASS_NAME, name ))
				 .that( value )
				 .isNull();
		} else {
			Truth.assertWithMessage( String.format( "field '%s.%s' has the wrong type", CLASS_NAME, name ))
			     .that( field.getType() )
			     .isEqualTo( clazz );
		}
		Truth.assertThat( setStaticFieldValue.test( field, null ))
		     .isTrue();
	}

	@Test
	void testFieldsArePrivate() {
		var clazz = getClazz( CLASS_NAME );
		Arrays.stream ( clazz.getDeclaredFields() )
		      .filter ( f -> !f.isSynthetic() )
		      .forEach( f -> Truth.assertWithMessage( String.format( "field '%s.%s' is not private", clazz.getSimpleName(), f.getName() ))
		    		              .that             ( Modifier.isPrivate( f.getModifiers() ))
		    		              .isTrue() );
	}
	@Test
	void testOnePrivateConstructor() {
		var clazz        = getClazz( CLASS_NAME );
		var constructors = Arrays.stream (clazz.getDeclaredConstructors()).collect( Collectors.toList() );
		
		Truth.assertThat( constructors )
		     .hasSize( 1 );
		
		var constructor  = constructors.get(0);
		Truth.assertWithMessage( String.format( "constructor in '%s' should be private", CLASS_NAME ))
			 .that( Modifier.isPrivate( constructor.getModifiers() ))
			 .isTrue();
	}
	@Test
	void testHasMethods() {
		var clazz = getClazz( CLASS_NAME );
		hasMethod( clazz, String.class, "toString"    );
		hasMethod( clazz, clazz,        "getInstance" );
	}
	@Test
	void testGetInstanceReturnsSameObject() {
		var one = PCSEChairLazy.getInstance();
		Truth.assertWithMessage( "getInstance() should return an object" )
		     .that( one )
		     .isNotNull();
		
		var two = PCSEChairLazy.getInstance();
		Truth.assertWithMessage( "getInstance() should return an object" )
		     .that( two )
		     .isNotNull();
		Truth.assertWithMessage( "getInstance() should return the same object" )
		     .that( two )
		     .isSameInstanceAs( one );
	}
	@Test
	public void testToString() {
		var a = PCSEChairLazy.getInstance();
		Truth.assertWithMessage( "getInstance() should return an object" )
		     .that( a )
		     .isNotNull();
		Truth.assertThat( a.toString() )
		     .isEqualTo( "Anton Riedl" );
	}
}
