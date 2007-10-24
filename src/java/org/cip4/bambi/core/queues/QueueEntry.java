/**
 * 
 */
package org.cip4.bambi.core.queues;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cip4.jdflib.core.JDFDoc;
import org.cip4.jdflib.jmf.JDFQueueEntry;

/**
 * simple QueueEntry / JDF pair
 * @author prosirai
 * 
 */
public class QueueEntry implements IQueueEntry
{
    private static Log log = LogFactory.getLog(QueueEntry.class.getName());
   
    private JDFDoc _theDoc;
    private JDFQueueEntry _theQueueEntry;
    
    public QueueEntry(JDFDoc doc, JDFQueueEntry qe) {
        super();
        log.info("constructing new QueueEntry");
        _theDoc=doc;
        _theQueueEntry=qe;
        if(_theDoc==null || _theQueueEntry==null)
            log.error("null elements in QueueEntry");
    }

    public JDFDoc getJDF() {
        return _theDoc;
    }
    
    public JDFQueueEntry getQueueEntry() {
        return _theQueueEntry;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String s="[QueueEntry: ] \nQueueEntry : ";
        s+=_theQueueEntry==null ?"null \n" : _theQueueEntry.getQueueEntryID() + "\n"+_theQueueEntry.toString();
        s+="\n Doc: "+_theDoc==null ?"null \n": _theDoc.toString();
        return s;
    }
    
}
