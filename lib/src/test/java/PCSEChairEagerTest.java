import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.truth.Truth;

//** EAGER ** //
class PCSEChairEagerTest {
	private static final String CLASS_NAME = "PCSEChairEager";
	private static       Object instanceValue = null;
	private static       Field  instanceField = null;
	
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
	private static final Function<Field,?> getStaticFieldValue = field -> getFieldValue.apply( null, field );

	@BeforeEach
	void testInstanceIsInitializedEagerly() {
		// has the instance field been read before? (field == null if it hasn't been read before) 
		// if not:
		//    get all static fields
		//    there should be only 1 static field (if not fail)
		//    this field should be final (if not fail).
		//    this field should have a value (if not fail: instance field is not eagerly initialized)
		//    this value should be of the singleton class (if not fail: not a singleton variable)
		//    if all OK: initialize instance field and value
		// read instance value:
		//    it should not be null (fail)
		//    it should be the same instance read earlier (if not fail: instance modified after initialization).
		if (instanceField == null) {
			var              clazz     = getClazz( CLASS_NAME );
			Predicate<Field> areStatic = f -> Modifier.isStatic( f.getModifiers() );
			var              fields    = getFields.apply( clazz, areStatic );
			
			Truth.assertWithMessage( "only one static variable should exist" )
			     .that( fields )
			     .hasSize( 1 );
	
			var field = fields.get(0);
			var name  = field.getName();
			Truth.assertWithMessage( String.format( "static field '%s.%s' should be final", CLASS_NAME, name ))
				 .that( Modifier.isFinal( field.getModifiers() ))
				 .isTrue();
		
			var value = getStaticFieldValue.apply( field );
			Truth.assertWithMessage( String.format( "field '%s.%s' is not eagerly initialized", CLASS_NAME, name ))
				 .that( value )
				 .isNotNull();
			
			Truth.assertWithMessage( String.format( "field '%s.%s' has the wrong type", CLASS_NAME, name ))
			     .that( value )
			     .isInstanceOf( clazz );
			
			instanceField = field;
			instanceValue = value;
		}
		var value = getStaticFieldValue.apply( instanceField );
		var name  = instanceField.getName();

		Truth.assertWithMessage( String.format( "unique instance '%s.%s' changed value after initialization", CLASS_NAME, name ))
		     .that( value )
		     .isSameInstanceAs( instanceValue );
	}

	@Test
	void testFieldsArePrivate() {
		var clazz = getClazz( CLASS_NAME );
		Arrays.stream ( clazz.getDeclaredFields() )
		      .filter ( f -> !f.isSynthetic() )
		      .forEach( f -> Truth.assertWithMessage( String.format( "field '%s.%s' is not private", CLASS_NAME, f.getName() ))
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
		var one = PCSEChairEager.getInstance();
		Truth.assertWithMessage( "getInstance() should return an object" )
	     .that( one )
	     .isNotNull();
		
		var two = PCSEChairEager.getInstance();
		Truth.assertWithMessage( "getInstance() should return an object" )
		     .that( two )
		     .isNotNull();
		Truth.assertWithMessage( "getInstance() should return the same object" )
		     .that( two )
		     .isSameInstanceAs( one );
	}
	@Test
	public void testToString() {
		var a = PCSEChairEager.getInstance();
		Truth.assertWithMessage( "getInstance() should return an object" )
		     .that( a )
		     .isNotNull();
		Truth.assertThat( a.toString() )
		     .isEqualTo( "Anton Riedl" );
	}
}
