/*
 * MongoLink, Object Document Mapper for Java and MongoDB
 *
 * Copyright (c) 2012, Arpinum or third-party contributors as
 * indicated by the @author tags
 *
 * MongoLink is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * MongoLink is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with MongoLink.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mongolink.domain.session;

import com.github.fakemongo.Fongo;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mongolink.*;
import org.mongolink.domain.criteria.*;
import org.mongolink.domain.mapper.ContextBuilder;
import org.mongolink.test.entity.*;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestsMongoSessionImpl {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void before() {
        final Fongo fongo = new Fongo("test");
        db =  spy(fongo.getDatabase("test"));
        session = createSession();
        session.start();
    }

    private MongoSessionImpl createSession() {
        ContextBuilder cb = new ContextBuilder("org.mongolink.test.simpleMapping");
        MongoSessionImpl session = new MongoSessionImpl(db, new CriteriaFactory());
        session.setMappingContext(cb.createContext());
        return session;
    }

    @Test
    @Ignore("voir si on peut remplacer le start and stop par autre choses")
    public void startAndStopASession() {
        MongoSessionImpl session = new MongoSessionImpl(db, new CriteriaFactory());

        session.start();
        session.stop();

        InOrder inorder = inOrder(db);
        //inorder.verify(db).requestStart();
        //inorder.verify(db).requestDone();
    }

    @Test
    public void canGetByAutoId() {
        final String id = createFakeAggregate("plop");

        FakeAggregate entity = session.get(id, FakeAggregate.class);

        assertThat(entity, notNullValue());
        assertThat(entity.getValue(), is("plop"));
    }

    @Test
    public void cantGetSomethingWichIsNotAnEntity() {
        exception.expect(MongoLinkError.class);
        session.get("pouet", Comment.class);
    }

    @Test
    public void canGetByNaturalId() {
        Document dbo = new Document();
        dbo.put("_id", "a natural key");
        fakeAggregatesWithNaturalId().insertOne(dbo);

        FakeAggregateWithNaturalId entity = session.get("a natural key", FakeAggregateWithNaturalId.class);

        assertThat(entity, notNullValue());
        assertThat(entity.getNaturalKey(), is("a natural key"));
    }

    @Test
    public void testDavid() {
        Document dbo = new Document();
        dbo.put("_id", "a natural key");
        fakeAggregatesWithNaturalId().insertOne(dbo);

        session.get("a natural key", FakeAggregateWithNaturalId.class);
        FakeAggregateWithNaturalId entity = session.get("a natural key", FakeAggregateWithNaturalId.class);

        entity.setValue("a new hope");
        FakeAggregateWithNaturalId anotherEntity = session.get("a natural key", FakeAggregateWithNaturalId.class);

        assertThat(anotherEntity.getValue(), is("a new hope"));
    }

    @Test
    public void canSave() {
        FakeAggregate entity = new FakeAggregate("value");

        session.save(entity);
        session.flush();

        assertThat(fakeAggregates().count(), is(1L));
        Document dbo = first();
        assertThat(dbo.get("value"), is("value"));
    }

    @Test
    public void cantSaveSomethingWichIsNotAnEntity() {
        exception.expect(MongoLinkError.class);
        session.save(new Comment());
    }

    @Test
    public void canUpdate() {
        final String id = createFakeAggregate("url de test");
        FakeAggregate entity = session.get(id, FakeAggregate.class);
        entity.setValue("un test de plus");

        session.flush();


        Document dbo = first();
        assertThat(dbo.get("value"), is("un test de plus"));
    }

    @Test
    public void canAutomaticalyUpdateOnStop() {
        final String id = createFakeAggregate("url de test");
        FakeAggregate fakeAggregate = session.get(id, FakeAggregate.class);
        fakeAggregate.setValue("some new and strange value");

        session.stop();

        Document dbObject = first();
        assertThat(dbObject.get("value"), is("some new and strange value"));
    }

    @Test
    public void returnNullIfNotFound() {
        FakeAggregateWithNaturalId entity = session.get("a natural key", FakeAggregateWithNaturalId.class);

        assertThat(entity, nullValue());
    }

    @Test
    public void canUpdateJustSavedEntityWithNaturalId() {
        FakeAggregateWithNaturalId entity = new FakeAggregateWithNaturalId("natural key");
        session.save(entity);
        entity.setValue("a value");

        session.flush();

        Document document = fakeAggregatesWithNaturalId().find(Filters.eq("_id", "natural key")).first();
        assertThat(document.get("value"), is("a value"));
    }

    @Test
    public void canUpdateJustSavedEntityWithAutoId() {
        FakeAggregate entity = new FakeAggregate("this is a value");
        session.save(entity);
        entity.setValue("a value");

        session.flush();


        assertThat(first().get("value"), is("a value"));
    }

    @Test
    public void canUpdateWithDiffStategy() {
        session.setUpdateStrategy(UpdateStrategies.DIFF);
        FakeAggregate entity = aAggregateWithIndexAt(3);
        setIndexInCollectionTo(entity, 4);

        entity.setValue("a value");
        session.flush();

        Document doc = (Document) fakeAggregates().find(Filters.eq("_id", new ObjectId(entity.getId()))).first();
        assertThat(doc.get("index"), is(4));
    }

    private Document first() {
        return fakeAggregates().find().first();
    }

    private void setIndexInCollectionTo(FakeAggregate entity, int index) {
        fakeAggregates().replaceOne(Filters.eq("_id", new ObjectId(entity.getId())), new Document("index", index));
    }

    private FakeAggregate aAggregateWithIndexAt(int index) {
        FakeAggregate entity = new FakeAggregate("this is a value");
        entity.setIndex(index);
        session.save(entity);
        session.flush();
        return entity;
    }


    @Test
    public void savingSetIdForAutoId() {
        FakeAggregate entity = new FakeAggregate("a value");

        session.save(entity);

        assertThat(entity.getId(), notNullValue());
    }

    @Test
    public void canGetCriteria() {
        final Criteria<FakeAggregate> criteria = session.createCriteria(FakeAggregate.class);

        assertThat(criteria, notNullValue());
    }

    @Test
    public void canGetByCriteria() {
        session.save(new FakeAggregate("this is a value"));
        session.save(new FakeAggregate("this is a value"));
        session.flush();
        final Criteria<FakeAggregate> criteria = session.createCriteria(FakeAggregate.class);

        List<FakeAggregate> list = criteria.list();

        assertThat(list.size(), is(2));
    }

    @Test
    public void cantGetByCriteriaWhenSessionNotStarted() {
        final MongoSessionImpl sessionNotStarted = createSession();

        exception.expect(MongoLinkError.class);
        sessionNotStarted.createCriteria(FakeAggregate.class);
    }

    @Test
    public void returnSameInstanceOnGetById() {
        Document dbo = new Document();
        dbo.put("_id", "a natural key");
        fakeAggregatesWithNaturalId().insertOne(dbo);

        FakeAggregateWithNaturalId first = session.get("a natural key", FakeAggregateWithNaturalId.class);
        FakeAggregateWithNaturalId second = session.get("a natural key", FakeAggregateWithNaturalId.class);

        assertThat(first, sameInstance(second));
    }

    @Test
    public void returnSameInstanceOnGetByCriteria() {
        final String id = createFakeAggregate("plop");
        final Criteria criteria = session.createCriteria(FakeAggregate.class);

        final FakeAggregate instanceById = session.get(id, FakeAggregate.class);
        final FakeAggregate instanceByCriteria = (FakeAggregate) criteria.list().get(0);

        assertThat(instanceById, sameInstance(instanceByCriteria));
    }

    @Test
    public void entitiesAreCachedUsingTheirTypeAndId() {
        session.save(new FakeAggregateWithNaturalId("cle unique"));
        session.save(new OtherEntityWithNaturalId("cle unique"));

        final FakeAggregateWithNaturalId entity = session.get("cle unique", FakeAggregateWithNaturalId.class);

        assertThat(entity, notNullValue());
    }

    @Test
    public void canClear() {
        final FakeAggregateWithNaturalId element = new FakeAggregateWithNaturalId("cle unique");
        session.save(element);

        session.clear();

        final FakeAggregateWithNaturalId elementFound = session.get("cle unique", FakeAggregateWithNaturalId.class);
        assertThat(elementFound, not(element));
    }

    @Test
    public void canGetAll() {
        createFakeAggregate("plop");
        createFakeAggregate("plap");

        List<FakeAggregate> entityList = session.getAll(FakeAggregate.class);

        assertThat(entityList, notNullValue());
        assertThat(entityList.size(), is(2));
    }

    @Test
    public void canDeleteEntity() {
        final FakeAggregateWithNaturalId entity = new FakeAggregateWithNaturalId("cle unique");
        session.save(entity);
        session.flush();

        session.delete(entity);
        session.flush();

        assertThat(fakeAggregates().count(), is(0L));
        assertThat(session.get("cle unique", FakeAggregateWithNaturalId.class), nullValue());
    }

    @Test
    public void cantDeleteEntityNotInCache() {
        final FakeAggregateWithNaturalId entity = new FakeAggregateWithNaturalId("cle unique");
        session.save(entity);
        session.clear();

        exception.expect(MongoLinkError.class);
        session.delete(entity);
    }

    @Test
    public void cantDeleteSomethingWichIsNotAnEntity() {
        exception.expect(MongoLinkError.class);
        session.delete(new Comment());
    }

    private String createFakeAggregate(final String value) {
        final ObjectId id = ObjectId.get();
        Document dbo = new Document();
        dbo.put("value", value);
        dbo.put("_id", id);
        fakeAggregates().insertOne(dbo);
        return id.toString();
    }

    private MongoCollection<Document> fakeAggregatesWithNaturalId() {
        return  db.getCollection("fakeaggregatewithnaturalid");
    }

    private MongoCollection<Document> fakeAggregates() {
        return  db.getCollection("fakeaggregate");
    }

    private MongoDatabase db;
    private MongoSessionImpl session;

}
