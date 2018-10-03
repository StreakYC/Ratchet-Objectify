package com.streak.ratchet.objectify;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.testing.RemoteSpannerHelper;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;

import com.streak.ratchet.Configuration;
import com.streak.ratchet.Query;
import com.streak.ratchet.RatchetReaderWriter;
import com.streak.ratchet.objectify.entities.OfyGrandparent;
import com.streak.ratchet.objectify.entities.OfyKeys;
import com.streak.ratchet.objectify.entities.OfyParent;
import com.streak.ratchet.objectify.translate.KeyTranslatorFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class IntegrationTests {
	private static final long ID_OFFSET = System.currentTimeMillis();
    private static final String INSTANCE_ID = "spannerdev";
	private static final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
	private static Closeable session;
    private static RemoteSpannerHelper spannerHelper;
    private static Database testDatabase;

    @BeforeClass
	public static void setup() throws Throwable {
		helper.setUp();

		ObjectifyFactory objectifyFactory = new ObjectifyFactory();
		objectifyFactory.register(OfyGrandparent.class);
		objectifyFactory.register(OfyParent.class);
		objectifyFactory.register(OfyKeys.class);
		ObjectifyService.init(objectifyFactory);
		session = ObjectifyService.begin();

		Configuration.INSTANCE.addTranslatorFactory(new KeyTranslatorFactory());
		Configuration.INSTANCE.register(OfyKeys.class);

        SpannerOptions options = SpannerOptions.newBuilder().build();
        spannerHelper = RemoteSpannerHelper.create(InstanceId.of(options.getProjectId(), INSTANCE_ID));
        List<String> ddl = new ArrayList<>(Configuration.INSTANCE.getMetadata(OfyKeys.class).getTable().ddl());
        testDatabase = spannerHelper.createTestDatabase(ddl);
        Configuration.INSTANCE.setSpannerProvider(() -> spannerHelper.getDatabaseClient(testDatabase));

	}

	@AfterClass
	public static void tearDown()  {
		try {
			if (session != null) {
				session.close();
			}
		}
		finally {
		    spannerHelper.cleanUp();
			helper.tearDown();
		}
	}

	@Test
	public void testRoundTrip() throws Throwable {
		Key<OfyGrandparent> gp = Key.create(OfyGrandparent.class, ID_OFFSET + 6L);
		Key<OfyParent> p = Key.create(gp, OfyParent.class, "Alvin");

		OfyKeys keyObj = new OfyKeys();
		keyObj.id = 4L;
		keyObj.parent = p;
		keyObj.sibling = Key.create(p, OfyKeys.class, 42L);
		keyObj.siblings = new ArrayList<>();
		keyObj.siblings.add(Key.create(p, OfyKeys.class, 5L));
		keyObj.siblings.add(Key.create(p, OfyKeys.class, 6L));
		keyObj.siblings.add(Key.create(p, OfyKeys.class, 7L));

		OfyKeys.Subclass sub = new OfyKeys.Subclass();
		sub.subSiblings = new ArrayList<>();
		sub.subSiblings.add(Key.create(p, OfyKeys.class, 10L));

		keyObj.sub = new ArrayList<>();
		keyObj.sub.add(sub);

		new RatchetReaderWriter().save(keyObj);

		System.out.println(new Query(OfyKeys.class)
			.current()
			.addMatch("id", keyObj.id)
			.addMatch("parent", keyObj.parent).asStatement());

		List<OfyKeys> results = new Query(OfyKeys.class)
			.current()
			.addMatch("id", keyObj.id)
			.addMatch("parent", keyObj.parent)
			.execute();
		assert results.size() == 1;

		Assert.assertEquals(keyObj, results.get(0));
	}

}
