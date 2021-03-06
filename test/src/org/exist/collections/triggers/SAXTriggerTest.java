/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-2014 The eXist Project
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.collections.triggers;

import static org.junit.Assert.*;

import org.exist.EXistException;
import org.exist.TestUtils;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.xmldb.DatabaseInstanceManager;
import org.exist.xmldb.IndexQueryService;
import org.exist.xmldb.XmldbURI;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;

public class SAXTriggerTest {

    private final static String DOCUMENT1_CONTENT = 
        "<test>"
        + "<item id='1'><price>5.6</price><stock>22</stock></item>"
        + "<item id='2'><price>7.4</price><stock>43</stock></item>"
        + "<item id='3'><price>18.4</price><stock>5</stock></item>"
        + "<item id='4'><price>65.54</price><stock>16</stock></item>"
        + "</test>";

    private final static String DOCUMENT2_CONTENT = 
            "<test>"
            + "<item id='1'><price>5.6</price><stock>22</stock></item>"
            + "</test>";

    private final static String DOCUMENT3_CONTENT = 
        "<test test=\"valueTest\">\n" +
        "    <item id=\"1\" test=\"valueTest\">\n" +
        "        <price test=\"valueTest\">5.6</price>\n" +
        "        <stock test=\"valueTest\">22</stock>\n" +
        "    </item>\n" +
        "</test>";
    
    private final static String COLLECTION_CONFIG = 
            "<exist:collection xmlns:exist='http://exist-db.org/collection-config/1.0'>"
            + "  <exist:triggers>"
            + "     <exist:trigger class='org.exist.collections.triggers.StoreTrigger'/>"
            + "  </exist:triggers>"
            + "</exist:collection>";


    private final static String BASE_URI = "xmldb:exist://";

    private final static String testCollection = "/db/triggers";
    
    @Rule
    public TestRule watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.println("\nStarting test: " + description.getMethodName());
        }
    };

    @Test
    public void test() throws EXistException {

        BrokerPool db = BrokerPool.getInstance();

        db.registerDocumentTrigger(AnotherTrigger.class);

        DBBroker broker = null;

        try {
            broker = db.get(db.getSecurityManager().getSystemSubject());

            Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "");

            Resource resource = root.createResource("data.xml", "XMLResource");
            resource.setContent(DOCUMENT1_CONTENT);
            root.storeResource(resource);

            assertEquals(3, AnotherTrigger.createDocumentEvents);

            assertEquals(26, AnotherTrigger.count);

            assertEquals(DOCUMENT1_CONTENT, AnotherTrigger.sb.toString());

        } catch (XMLDBException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (broker != null)
                broker.release();
        }
    }

    @Test
    public void saxEventModifications() throws EXistException {

        BrokerPool db = BrokerPool.getInstance();

        db.registerDocumentTrigger(StoreTrigger.class);

        DBBroker broker = null;

        try {
            broker = db.get(db.getSecurityManager().getSystemSubject());

            Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "");

            Resource resource = root.createResource("data.xml", "XMLResource");
            resource.setContent(DOCUMENT2_CONTENT);
            root.storeResource(resource);

            resource = root.createResource("data.xml", "XMLResource");

            assertEquals(DOCUMENT3_CONTENT, resource.getContent().toString());

        } catch (XMLDBException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (broker != null)
                broker.release();
        }
    }

    @Test
    public void saxEventModificationsAtXConf() throws EXistException {

        BrokerPool db = BrokerPool.getInstance();

        DBBroker broker = null;

        try {
            broker = db.get(db.getSecurityManager().getSystemSubject());

            Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "");

            IndexQueryService idxConf = (IndexQueryService) root.getService("IndexQueryService", "1.0");
            idxConf.configureCollection(COLLECTION_CONFIG);

            Resource resource = root.createResource("data.xml", "XMLResource");
            resource.setContent(DOCUMENT2_CONTENT);
            root.storeResource(resource);

            resource = root.createResource("data.xml", "XMLResource");

            assertEquals(DOCUMENT3_CONTENT, resource.getContent().toString());

        } catch (XMLDBException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (broker != null)
                broker.release();
        }
    }

    @After
    public void cleanDB() {
        try {
            Collection config = DatabaseManager.getCollection(BASE_URI + "/db/system/config" + testCollection, "admin", null);
            if (config != null) {
                CollectionManagementService mgmt = (CollectionManagementService) config.getService("CollectionManagementService", "1.0");
                mgmt.removeCollection(".");
            }
            Collection root = DatabaseManager.getCollection(BASE_URI + testCollection, "admin", "");
            Resource resource = root.getResource("messages.xml");
            if (resource != null) {
                root.removeResource(resource);
            }
            resource = root.getResource("data.xml");
            if (resource != null) {
                root.removeResource(resource);
            }
        } catch (XMLDBException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @BeforeClass
    public static void initDB() {
        // initialize XML:DB driver
        try {
            Class<?> cl = Class.forName("org.exist.xmldb.DatabaseImpl");
            Database database = (Database) cl.newInstance();
            database.setProperty("create-database", "true");
            DatabaseManager.registerDatabase(database);

            Collection root = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, "admin", "");
            CollectionManagementService mgmt = (CollectionManagementService) root.getService("CollectionManagementService", "1.0");
            Collection testCol = mgmt.createCollection("triggers");

            for (int i = 1; i <= 2; i++) {
                mgmt = (CollectionManagementService) testCol.getService("CollectionManagementService", "1.0");
                testCol = mgmt.createCollection("sub" + i);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @AfterClass
    public static void closeDB() {
        TestUtils.cleanupDB();
        try {
            Collection root = DatabaseManager.getCollection(XmldbURI.LOCAL_DB, "admin", "");
            DatabaseInstanceManager mgr = (DatabaseInstanceManager) root.getService("DatabaseInstanceManager", "1.0");
            mgr.shutdown();
        } catch (XMLDBException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
