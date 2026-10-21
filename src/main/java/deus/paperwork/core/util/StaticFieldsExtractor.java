package deus.paperwork.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class StaticFieldsExtractor {

	public static List<String> getStaticFieldNames(Class<?> clazz) {
		List<String> staticFieldNames = new ArrayList<>();
		Field[] fields = clazz.getDeclaredFields();

		for (Field field : fields) {

			if (Modifier.isStatic(field.getModifiers())) {
				staticFieldNames.add(field.getName());
			}
		}

		return staticFieldNames;
	}

	public static String[] extractor(Class<?> clazz) {
		List<String> staticFields = getStaticFieldNames(clazz);
		return staticFields.toArray(new String[0]);
	}

}
