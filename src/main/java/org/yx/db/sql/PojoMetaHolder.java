package org.yx.db.sql;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.yx.asm.AsmUtils;
import org.yx.db.annotation.Column;
import org.yx.db.annotation.Table;
import org.yx.exception.SumkException;
import org.yx.log.Log;
import org.yx.util.StringUtils;

public class PojoMetaHolder {

	private static Map<Class<?>, PojoMeta> pojoMetas = new ConcurrentHashMap<>();
	private static Map<String, PojoMeta> tableMetas = new ConcurrentHashMap<>();

	public static PojoMeta getTableMeta(String table) {
		return tableMetas.get(table);
	}

	public static PojoMeta getPojoMeta(Class<?> clz) {
		if (clz == null || clz.isInterface() || clz == Object.class) {
			return null;
		}
		Class<?> tmp = clz;
		while (tmp != Object.class) {
			PojoMeta m = pojoMetas.get(tmp);
			if (m != null) {
				return m;
			}
			tmp = tmp.getSuperclass();
		}
		return null;
	}

	public static void resolve(Class<?> pojoClz) {

		if (pojoClz.isInterface() || pojoClz.isEnum() || AsmUtils.notPublicOnly(pojoClz.getModifiers())) {
			return;
		}
		Table table = pojoClz.getAnnotation(Table.class);
		if (table == null) {
			return;
		}
		Map<String, Field> map = new HashMap<>();
		Class<?> clz = pojoClz;
		while (clz != Object.class) {
			Field[] fields = clz.getDeclaredFields();
			for (Field f : fields) {
				map.putIfAbsent(f.getName(), f);
			}
			clz = clz.getSuperclass();
		}
		List<ColumnMeta> list = new LinkedList<>();
		Collection<Field> set = map.values();
		for (Field f : set) {
			Column c = f.getAnnotation(Column.class);
			f.setAccessible(true);
			if (c == null) {
				list.add(new ColumnMeta(f, ColumnType.NORMAL));
				continue;
			}
			ColumnMeta cm = new ColumnMeta(f, c.columnType());
			cm.setColumnOrder(c.columnOrder());
			cm.setDbColumn(StringUtils.isEmpty(c.value()) ? f.getName().toLowerCase() : c.value());
			list.add(cm);
		}
		if (list.isEmpty()) {
			Log.get("pojo").debug("{}'s column is empty", pojoClz.getName());
			return;
		}
		Collections.sort(list);
		PojoMeta tm = new PojoMeta(table, list.toArray(new ColumnMeta[0]), pojoClz);
		if (tm.getPrimaryIDs().length == 0) {
			SumkException.throwException(56456456, pojoClz.getName() + " has no primary key");
		}
		pojoMetas.put(pojoClz, tm);
		tableMetas.put(tm.getTableName(), tm);
	}
}
