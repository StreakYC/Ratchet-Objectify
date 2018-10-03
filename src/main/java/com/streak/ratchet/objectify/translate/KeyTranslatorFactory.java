package com.streak.ratchet.objectify.translate;

import com.googlecode.objectify.Key;
import com.streak.ratchet.AbstractMetadataField;
import com.streak.ratchet.translate.Translator;
import com.streak.ratchet.translate.TranslatorFactory;

import java.lang.reflect.Type;

import static com.streak.ratchet.Utils.erase;

public class KeyTranslatorFactory implements TranslatorFactory<Key<?>> {

	@Override public Translator<Key<?>> create(AbstractMetadataField metadataField) {
		return new KeyTranslator(metadataField);
	}

	@Override public boolean accepts(Type type) {
		return Key.class.isAssignableFrom(erase(type));
	}
}
