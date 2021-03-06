/*
 * Copyright 2017. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.tzolov.geode.jta.narayana.lrco;

import com.arjuna.ats.jta.resources.LastResourceCommitOptimisation;
import org.apache.geode.LogWriter;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.TransactionId;
import org.apache.geode.internal.cache.GemFireCacheImpl;
import org.apache.geode.internal.cache.TXManagerImpl;
import org.apache.geode.internal.cache.TXStateProxy;
import org.apache.geode.internal.i18n.LocalizedStrings;

import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * Implements the {@link LastResourceCommitOptimisation} marker interface. It extends the {@link XAResource} interface
 * but provides no additional methods. When enlisting the resource via method Transaction.enlistResource, Narayana
 * ensures that only a single instance of this type of participant is used within each transaction. Your resource
 * is driven last in the commit protocol, and no invocation of method prepare occurs.
 * <p>
 * By default an attempt to enlist more than one instance of a LastResourceCommitOptimisation class will fail and false
 * will be returned from Transaction.enlistResource. This behavior can be overridden by setting the
 * <b>com.arjuna.ats.jta.allowMultipleLastResources</b> to true.
 * <p>
 * <p>
 * Note: To large extend the {@link GeodeNarayanaLrcoResource} implementation reuses the
 * {@link org.apache.geode.internal.ra.spi.JCALocalTransaction JCALocalTransaction} code:
 * https://github.com/apache/geode/blob/develop/geode-core/src/jca/java/org/apache/geode/internal/ra/spi/JCALocalTransaction.java
 *
 * @author Christian Tzolov (christian.tzolov@gmail.com)
 */
public class GeodeNarayanaLrcoResource implements LastResourceCommitOptimisation {

    private volatile GemFireCacheImpl cache;
    private volatile TXManagerImpl gfTxMgr;
    private volatile TransactionId tid;
    private volatile boolean initDone = false;

    @Override
    public void commit(Xid xid, boolean b) throws XAException {
        LogWriter logger = this.cache.getLogger();
        if (logger.fineEnabled()) {
            logger.fine("GeodeNarayanaLrcoResource:invoked commit");
        }

        TXStateProxy tsp = this.gfTxMgr.getTXState();
        if (tsp != null && this.tid != tsp.getTransactionId()) {
            throw new IllegalStateException("Local Transaction associated with Tid = " + this.tid + " attempting to commit a different transaction");
        } else {
            try {
                this.gfTxMgr.commit();
                this.tid = null;
            } catch (Exception var4) {
                throw new XAException(var4.toString());
            }
        }
    }

    @Override
    public void end(Xid xid, int i) throws XAException {
        throw new XAException("End called on Last Resource Txt!" + xid + ", i=" + i);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        throw new XAException("Forget called on Last Resource Txt!" + xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return 0;
    }

    @Override
    public boolean isSameRM(XAResource xaResource) throws XAException {
        return xaResource instanceof GeodeNarayanaLrcoResource;
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        throw new XAException("Prepare called on Last Resource Txt!" + xid);
    }

    @Override
    public Xid[] recover(int i) throws XAException {
        return new Xid[0];
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        TXStateProxy tsp = this.gfTxMgr.getTXState();
        if (tsp != null && this.tid != tsp.getTransactionId()) {
            throw new IllegalStateException("Local Transaction associated with Tid = " + this.tid + " attempting to commit a different transaction");
        } else {
            LogWriter logger = this.cache.getLogger();
            if (logger.fineEnabled()) {
                logger.fine("GeodeNarayanaLrcoResource:invoked rollback");
            }

            try {
                this.gfTxMgr.rollback();
            } catch (IllegalStateException var8) {
                if (!var8.getMessage().equals(LocalizedStrings.TXManagerImpl_THREAD_DOES_NOT_HAVE_AN_ACTIVE_TRANSACTION.toLocalizedString())) {
                    throw new XAException(var8.toString());
                }
            } catch (Exception var9) {
                throw new XAException(var9.toString());
            } finally {
                this.tid = null;
            }
        }
    }

    @Override
    public boolean setTransactionTimeout(int i) throws XAException {
        return false;
    }

    @Override
    public void start(Xid xid, int i) throws XAException {
        try {
            if (!this.initDone || this.cache.isClosed()) {
                this.init();
            }

            LogWriter logger = this.cache.getLogger();
            if (logger.fineEnabled()) {
                logger.fine("GeodeNarayanaLrcoResource::start:" + xid + ", i=" + i);
            }

            TransactionManager tm = this.cache.getJTATransactionManager();

            if (logger.fineEnabled()) {
                logger.fine("Start Geode Transaction using TransactionManager: " + tm.getClass());
            }

            if (this.tid != null) {
                throw new XAException(" A transaction is already in progress");
            } else {
                if (tm != null && tm.getTransaction() != null) {
                    if (logger.fineEnabled()) {
                        logger.fine("GeodeNarayanaLrcoResource: JTA transaction is on");
                    }

                    TXStateProxy tsp = this.gfTxMgr.getTXState();
                    if (tsp != null) {
                        throw new XAException("GemFire is already associated with a transaction");
                    }

                    this.gfTxMgr.begin();
                    tsp = this.gfTxMgr.getTXState();
                    tsp.setJCATransaction();
                    this.tid = tsp.getTransactionId();
                    if (logger.fineEnabled()) {
                        logger.fine("GeodeNarayanaLrcoResource:begun GFE transaction");
                    }
                } else if (logger.fineEnabled()) {
                    logger.fine("GeodeNarayanaLrcoResource: JTA Transaction does not exist.");
                }

            }
        } catch (SystemException var4) {
            throw new XAException(var4.getMessage());
        }
    }

    private void init() throws SystemException {
        this.cache = (GemFireCacheImpl) CacheFactory.getAnyInstance();
        LogWriter logger = this.cache.getLogger();
        if (logger.fineEnabled()) {
            logger.fine("GeodeNarayanaLrcoResource:init. Inside init");
        }

        this.gfTxMgr = this.cache.getTxManager();
        this.initDone = true;
    }
}
