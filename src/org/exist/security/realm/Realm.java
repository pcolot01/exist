/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2010 The eXist Project
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
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.security.realm;

import java.util.Collection;

import org.exist.EXistException;
import org.exist.security.Group;
import org.exist.security.Account;
import org.exist.security.management.AccountsManagement;
import org.exist.security.management.GroupsManagement;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;

/**
 * @author <a href="mailto:shabanovd@gmail.com">Dmitriy Shabanov</a>
 *
 */
public interface Realm<A extends Account, G extends Group> extends AuthenticatingRealm, AuthorizingRealm, AccountsManagement<A>, GroupsManagement<G> {
	
	public String getId();
	
	public Collection<A> getAccounts();
	
	public Collection<G> getRoles();

	public void startUp(DBBroker broker) throws EXistException;

	public BrokerPool getDatabase();
}