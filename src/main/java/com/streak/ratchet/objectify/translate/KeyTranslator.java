package com.streak.ratchet.objectify.translate;

import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.Statement.Builder;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.Code;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.impl.KeyMetadata;
import com.streak.ratchet.AbstractMetadataField;
import com.streak.ratchet.Utils;
import com.streak.ratchet.Utils.WrappedField;
import com.streak.ratchet.schema.SpannerField;
import com.streak.ratchet.translate.Translator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.streak.ratchet.Utils.erase;
import static com.streak.ratchet.Utils.innerType;

public class KeyTranslator implements Translator<Key<?>> {
	private final Class<?> destination;
	private final AbstractMetadataField metadataField;
	private List<KeyPart> keyParts = new ArrayList<>();

	public KeyTranslator(AbstractMetadataField metadataField) {
		this.metadataField = metadataField;
		this.destination = erase(innerType(metadataField.getType()));
		Class<?> partType = erase(innerType(metadataField.getType()));
		makeKeyParts(partType, null, null);

	}

	private void makeKeyParts(Class<?> partType, String partName, Class<?> type) {
		KeyMetadata<?> keyMetadata = ObjectifyService.factory()
													 .getMetadata(partType)
													 .getKeyMetadata();
		keyParts.add(new KeyPart(keyMetadata.getIdFieldType(), partName, keyMetadata.getKind(), type));
		if (keyMetadata.hasParentField()) {
			String parentFieldName = keyMetadata.getParentFieldName();
			WrappedField wrappedParentField = Utils.getFieldFromClassOrSuper(partType, parentFieldName);
			Class<?> parentType = Utils.getTypeFromKeyField(wrappedParentField.field);
			if (!wrappedParentField.klass.equals(parentType)) {
				makeKeyParts(parentType, parentType.getSimpleName(), parentType);
			}
			// TODO this feels like all sorts of wrong
		}
	}

	@Override public void addValueToMutation(WriteBuilder builder,
											 String name,
											 Key<?> value) {
		if (null == value) {
			return;
		}
		Key<?> currentValueLevel = value;
		for (KeyPart part : this.keyParts) {
			if (part.keyPartClass == String.class) {
				builder.set(part.fieldName(name)).to(currentValueLevel.getName());
			}
			else {
				builder.set(part.fieldName(name)).to(currentValueLevel.getId());
			}
			currentValueLevel = currentValueLevel.getParent();
		}
	}

	@Override public void addValueToStatement(Builder builder, String name, int index, Key<?> value) {
		if (null == value) {
			return;
		}
		Key<?> currentValueLevel = value;
		for (KeyPart part : this.keyParts) {
			if (part.keyPartClass == String.class) {
				builder.bind(part.fieldName(name) + "_" + index).to(currentValueLevel.getName());
			}
			else {
				builder.bind(part.fieldName(name) + "_" + index).to(currentValueLevel.getId());
			}
			currentValueLevel = currentValueLevel.getParent();
		}
	}

	@Override public void addToKeyBuilder(com.google.cloud.spanner.Key.Builder builder, Key<?> value) {
		while (null != value) {
			String name = value.getName();
			if (null != name) {
				builder.append(name);
			}
			else {
				builder.append(value.getId());
			}
			value = value.getParent();
		}
	}

	@Override public List<Object> values(Key<?> value) {
		List<Object> ret = new ArrayList<>();
		while (null != value) {
			String name = value.getName();
			if (null != name) {
				ret.add(name);
			}
			else {
				ret.add(value.getId());
			}
			value = value.getParent();
		}
		return ret;
	}

	@Override
	public List<SpannerField> asSpannerFields() {
		return this.keyParts.stream().map(
			part -> new SpannerField(
				metadataField.getTable(),
				part.fieldName(metadataField.getName()),
				part.keyPartClass == String.class ? "STRING(MAX)" : "Int64",
				metadataField.isKey,
				metadataField.isTip,
				metadataField.isVersion,
				metadataField.getNotNull())
		).collect(Collectors.toList());
	}

	@Override public Key<?> translateSpannerValueToFieldType(Struct struct) {
		return translateSpannerValueToFieldType(struct.getStructList(metadataField.getName()), struct.getColumnType(metadataField.getName()));
	}

	@Override
	public Key<?> translateSpannerValueToFieldType(Object item,
												   Type innerType) {
		if (null == innerType ||
			innerType.getCode() != Code.ARRAY ||
			innerType.getArrayElementType().getCode() != Code.STRUCT) {
			throw new RuntimeException("Type not supported: " + innerType);
		}
		Struct struct = ((List<Struct>) item).get(0);
		Key currentKey = null;

		// TODO - we know the actual field names now, use those
		// this won't blow up, nahhhh
		String prefix = struct.getType().getStructFields().get(0).getName().split("_")[0];

		List<KeyPart> reversedKeyParts = ImmutableList.copyOf(keyParts).reverse();
		for (KeyPart part : reversedKeyParts) {
			String fieldName = part.fieldName(prefix);
			if (struct.isNull(fieldName)) {
				return null;
			}

			Class<?> kindClass = part.type != null ? part.type : destination;
			if (part.keyPartClass == String.class) {
				currentKey = Key.create(currentKey, kindClass, struct.getString(fieldName));
			}
			else {
				currentKey = Key.create(currentKey, kindClass, struct.getLong(fieldName));
			}
		}
		return currentKey;
	}

	class KeyPart {
		private final Class<?> type;
		Class<?> keyPartClass;
		String name;
		String kind;

		KeyPart(Class<?> partType, String partName, String partKind, Class<?> type) {
			this.keyPartClass = partType;
			name = partName;
			kind = partKind;
			this.type = type;
		}

		public String fieldName(String prefix) {
			String fieldName = prefix;
			if (name != null) {
				fieldName += "_" + name;
			}
			if (keyPartClass == String.class) {
				fieldName += "_name";
			}
			else {
				fieldName += "_id";
			}
			return fieldName;
		}
	}
}
