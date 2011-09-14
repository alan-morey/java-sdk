/**
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *    Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 *    the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *    Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 *    promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.force.sdk.codegen;

import static org.testng.Assert.*;

import java.io.IOException;
import java.util.*;

import javax.jdo.JDODetachedFieldAccessException;
import javax.jdo.identity.StringIdentity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.datanucleus.ObjectManager;
import org.testng.annotations.*;

import com.force.sdk.codegen.entities.AccountCustomFields;
import com.force.sdk.codegen.entities.NewCustomObject;
import com.force.sdk.jpa.model.BaseForceObject;
import com.force.sdk.jpa.model.ForceOwner;
import com.force.sdk.qa.util.TestContext;
import com.force.sdk.qa.util.jpa.BaseJPAFTest;
import com.sforce.ws.ConnectionException;

/**
 * Tests that created JPA Java classes 
 * (i.e. those extending the generated classes)
 * are enabled for JPA CRUD.
 *
 * @author Fiaz Hossain
 * @author Tim Kral
 */
public class CreatedClassCRUDFTest extends BaseJPAFTest {
    
    EntityManager em2;
    EntityManager em3;

    @Override
    @BeforeSuite(alwaysRun = true)
    public void suiteSetup() throws IOException {
        Properties persistenceUnitName = new Properties();
        persistenceUnitName.setProperty(TestContext.PERSISTENCE_UNIT_NAME, "CodeGenTest");
        TestContext.get().addTestProps(persistenceUnitName);
        super.suiteSetup();
    }
    
    @Override
    @BeforeClass
    public void initialize() throws IOException, ConnectionException {
        super.initialize();
        em2 = getAdditionalEntityManagers().get(TestContext.get().getPersistenceUnitName() + "2");
        em3 = getAdditionalEntityManagers().get(TestContext.get().getPersistenceUnitName() + "3");
    }
    
    @Override
    @AfterSuite(alwaysRun = true)
    public void suiteTeardown() throws Exception {
        super.suiteTeardown();
        // Release the context because we set a custom persistence unit name in suiteSetup
        TestContext.release();
    }
    
    @Override
    public Set<String> getAdditionalPersistenceUnitNames() {
        return new HashSet<String>(Arrays.asList(new String[] {
                TestContext.get().getPersistenceUnitName() + "2",
                TestContext.get().getPersistenceUnitName() + "3"
        }));
    }

    
    @Test
    public void testStandardObjectExtensionCRUD() {
        testStandardObjectExtensionCRUDInternal(em);
    }

    @Test
    public void testStandardObjectExtensionCRUDOptimistic() {
        testStandardObjectExtensionCRUDInternal(em2);
    }
    
    @Test
    public void testStandardObjectExtensionCRUDAllOrNothing() {
        testStandardObjectExtensionCRUDInternal(em3);
    }
    
    @SuppressWarnings("unchecked")
    private void testStandardObjectExtensionCRUDInternal(EntityManager entityManager) {
        deleteAll("Case");
        deleteAll("Opportunity");
        deleteAll(AccountCustomFields.class);
        
        final String name = "Sample Account";
        AccountCustomFields entity = new AccountCustomFields();
        entity.setName(name);
        entity.setSomeCustomField("value1");
        
        persistForceObject(entityManager, entity);

        // First try with find()
        entity = entityManager.find(entity.getClass(), entity.getId());
        assertEquals(entity.getName(), name, "Name did not match.");
        
        // Then try with query()
        List<AccountCustomFields> results = entityManager.createQuery("Select t From AccountCustomFields t").getResultList();
        entity = results.iterator().next();
        assertEquals(entity.getName(), name, "Name did not match.");
        
        // Now we need to make sure update works on MappsedSuperclass entities
        final String newName = "Renamed Sample Account";
        entity.setName(newName);
        mergeForceObject(entityManager, entity);
        
        // Read back upated object
        results = entityManager.createQuery("Select t From AccountCustomFields t where name = ?1")
                                    .setParameter(1, newName).getResultList();
        AccountCustomFields entity1 = results.iterator().next();
        assertEquals(entity1.getId(), entity.getId(), "Ids did not match.");
    }
    
    @Test
    public void testEagerlyFetchedOwnerField() {
        deleteAll("Case");
        deleteAll("Opportunity");
        deleteAll(AccountCustomFields.class);
        
        final String name = "testEagerlyFetchedOwnerField";
        AccountCustomFields entity = new AccountCustomFields();
        entity.setName(name);
        
        persistForceObject(em, entity);

        EntityTransaction tx = em.getTransaction();
        tx.begin();
        entity = em.find(entity.getClass(), entity.getId());
        
        assertNotNull(entity.getForceOwner(), "Force.com Owner field was not found");
        assertNotNull(entity.getForceOwner().getId(), "Force.com Owner id was not found");
        assertNotNull(entity.getForceOwner().getName(), "Force.com Owner name was not found");
        
        // Assert that the Force.com Owner is in the cache
        ObjectManager om = (ObjectManager) em.getDelegate();
        assertNotNull(om.getObjectFromCache(new StringIdentity(ForceOwner.class, entity.getForceOwner().getId())));
        tx.commit();
        
        // Force.com Owner is eagerly fetched on AccountCustomFields so
        // it should be available outside of the find transaction
        assertNotNull(entity.getForceOwner(), "Force.com Owner field did not get eagerly fetched");
        assertNotNull(entity.getForceOwner().getId(), "Force.com Owner id did not get eagerly fetched");
        assertNotNull(entity.getForceOwner().getName(), "Force.com Owner name did not get eagerly fetched");
    }
    
    @Test
    public void testCustomObjectCRUD() {
        testCustomObjectCRUDInternal(em);
    }

    @Test
    public void testCustomObjectCRUDOptimistic() {
        testCustomObjectCRUDInternal(em2);
    }
    
    @Test
    public void testCustomObjectCRUDAllOrNothing() {
        testCustomObjectCRUDInternal(em3);
    }
    
    @SuppressWarnings("unchecked")
    private void testCustomObjectCRUDInternal(EntityManager entityManager) {
        deleteAll(NewCustomObject.class);
        
        final String name = "Sample CustomObject";
        NewCustomObject entity = new NewCustomObject();
        entity.setName(name);
        entity.setSomeCustomField("value1");
        
        persistForceObject(entityManager, entity);

        // First try with find()
        entity = entityManager.find(entity.getClass(), entity.getId());
        assertEquals(entity.getName(), name, "Name did not match.");
        
        // Then try with query()
        List<NewCustomObject> results = entityManager.createQuery("Select t From NewCustomObject t").getResultList();
        entity = results.iterator().next();
        assertEquals(entity.getName(), name, "Name did not match.");
        
        // Now we need to make sure update works on MappsedSuperclass entities
        final String newName = "Renamed CustomObject";
        entity.setName(newName);
        mergeForceObject(entityManager, entity);
        
        // Read back upated object
        results = entityManager.createQuery("Select t From NewCustomObject t where name = ?1")
                                    .setParameter(1, newName).getResultList();
        NewCustomObject entity1 = results.iterator().next();
        assertEquals(entity1.getId(), entity.getId(), "Ids did not match.");
    }
    
    private void persistForceObject(EntityManager entityManager, BaseForceObject entity) {
        EntityTransaction tx = entityManager.getTransaction();
        tx.begin();
        entityManager.persist(entity);
        tx.commit();
        assertNotNull(entity.getId(), entity.getClass().getName() + " ID was not generated.");
    }
    
    private void mergeForceObject(EntityManager entityManager, BaseForceObject entity) {
        EntityTransaction tx = entityManager.getTransaction();
        tx.begin();
        entityManager.merge(entity);
        tx.commit();
    }
    
    @Test
    public void testLazilyFetchedOwnerField() {
        deleteAll(NewCustomObject.class);
        
        final String name = "testLazilyFetchedOwnerField";
        NewCustomObject entity = new NewCustomObject();
        entity.setName(name);
        
        persistForceObject(em, entity);

        EntityTransaction tx = em.getTransaction();
        tx.begin();
        entity = em.find(entity.getClass(), entity.getId());
        tx.commit();
        
        // Force.com Owner is lazily fetched on NewCustomObject so
        // it should not be available outside of the find transaction
        try {
            entity.getForceOwner();
            fail("Force.com Owner field should not be available outside of the find transaction because it is lazily fetched.");
        } catch (JDODetachedFieldAccessException expected) {
            // Expected
        }
    }
}
