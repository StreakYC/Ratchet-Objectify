package com.streak.ratchet.objectify.entities;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Parent;
import com.streak.ratchet.Annotations.Key;
import com.streak.ratchet.Annotations.Table;
import com.streak.ratchet.Annotations.Tip;
import com.streak.ratchet.Annotations.Version;

import java.util.List;
import java.util.Objects;

@Entity
@Table
public class OfyKeys {
	@Key @Id
	public Long id;

	@Key
	@Parent
	public com.googlecode.objectify.Key<OfyParent> parent;

	@Version @Ignore
	public Long version;

	@Tip @Ignore
	public Boolean tip;

	public com.googlecode.objectify.Key<OfyKeys> sibling;
	public List<com.googlecode.objectify.Key<OfyKeys>> siblings;

	public List<Subclass> sub;

	@Override public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		OfyKeys ofyKeys = (OfyKeys) o;
		return Objects.equals(id, ofyKeys.id) &&
			Objects.equals(parent, ofyKeys.parent) &&
			Objects.equals(version, ofyKeys.version) &&
			Objects.equals(tip, ofyKeys.tip) &&
			Objects.equals(sibling, ofyKeys.sibling) &&
			Objects.equals(siblings, ofyKeys.siblings) &&
			Objects.equals(sub, ofyKeys.sub);
	}

	@Override public int hashCode() {

		return Objects.hash(id, parent, version, tip, sibling, siblings, sub);
	}

	public static class Subclass {
		public List<com.googlecode.objectify.Key<OfyKeys>> subSiblings;

		@Override public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Subclass subclass = (Subclass) o;
			return Objects.equals(subSiblings, subclass.subSiblings);
		}

		@Override public int hashCode() {

			return Objects.hash(subSiblings);
		}

		public Subclass() {
		}
	}
}
