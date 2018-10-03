package com.streak.ratchet.objectify.entities;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class OfyParent {
	@Id
	String nameId;

	@Parent
	Key<OfyGrandparent> parent;
}
