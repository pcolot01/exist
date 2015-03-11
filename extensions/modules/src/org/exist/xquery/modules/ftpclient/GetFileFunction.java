package org.exist.xquery.modules.ftpclient;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.io.IOUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import org.exist.dom.QName;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.BinaryValueFromInputStream;
import org.exist.xquery.value.Base64BinaryValueType;
import org.exist.xquery.value.IntegerValue;

import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.xmldb.api.*;
import org.exist.xmldb.EXistResource;

/**
 *
 * @author WStarcev
 * @author Adam Retter <adam@existsolutions.com>
 */
public class GetFileFunction extends BasicFunction {

    private static final FunctionParameterSequenceType CONNECTION_HANDLE_PARAM = new FunctionParameterSequenceType("connection-handle", Type.LONG, Cardinality.EXACTLY_ONE, "The connection handle");
    private static final FunctionParameterSequenceType REMOTE_DIRECTORY_PARAM = new FunctionParameterSequenceType("remote-directory", Type.STRING, Cardinality.EXACTLY_ONE, "The remote directory");
    private static final FunctionParameterSequenceType FILE_NAME_PARAM = new FunctionParameterSequenceType("file-name", Type.STRING, Cardinality.EXACTLY_ONE, "File name");
    
    private static final Logger log = Logger.getLogger(GetFileFunction.class);
    
	private InputStream fileIs;
	private BinaryValueFromInputStream resultFromFile;
	private long key;
	
    public final static FunctionSignature signature = new FunctionSignature(
        new QName("get-binary-file", FTPClientModule.NAMESPACE_URI, FTPClientModule.PREFIX),
        "Get binary file from the FTP Server.",
        new SequenceType[] {
            CONNECTION_HANDLE_PARAM,
            REMOTE_DIRECTORY_PARAM,
            FILE_NAME_PARAM
        },
        new FunctionReturnSequenceType(Type.BASE64_BINARY, Cardinality.ZERO_OR_ONE, "File retrieved from the server.")
    );

    public GetFileFunction(XQueryContext context){
        super(context, signature);
		key = System.currentTimeMillis();
		log.warn("FTP GETFILEFUNCTION CREATED ["+key+"]");
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        
        Sequence result = Sequence.EMPTY_SEQUENCE;
        
        long connectionUID = ((IntegerValue)args[0].itemAt(0)).getLong();
        FTPClient ftp = FTPClientModule.retrieveConnection(context, connectionUID);

        if(ftp != null) {
            String remoteDirectory = args[1].getStringValue();
            String fileName = args[2].getStringValue();
            
            result = getBinaryFile(ftp, remoteDirectory, fileName);
			log.warn("FTP server get binary File: " + fileName + " from " + remoteDirectory + " ["+key+"]");
        }
        
        return result;
    }

    private Sequence getBinaryFile(FTPClient ftp, String remoteDirectory, String fileName) throws XPathException {
        
        Sequence result = Sequence.EMPTY_SEQUENCE;
        
        try {
            ftp.changeWorkingDirectory(remoteDirectory);
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
			
			/* using byte array internally */
			InputStream bisFTP = ftp.retrieveFileStream(fileName);
            byte[] bytes = IOUtils.toByteArray(bisFTP);
			InputStream bis = new ByteArrayInputStream(bytes);
            result = BinaryValueFromInputStream.getInstance(context, new Base64BinaryValueType(), bis);
			int repCode = ftp.getReplyCode();
			String[] replyStrAr = ftp.getReplyStrings();
			//close resources used by ftp
			bisFTP.close();
			ftp.completePendingCommand();
			String replyStr = "";
			for (String rep : replyStrAr) {
				replyStr += rep +"; ";
			}
			log.warn("FTP returns with [" + repCode + "] and " + "reply message: [" +replyStr+"]"); 
        } catch(IOException ioe) {
            log.error(ioe.getMessage(), ioe);
        }

        return result;
    }
	

	/* 
	* Method usage of loading byte array to existdb (example)
	private void storeFile(String path, String filename, String nameAppendix, byte[] data){
		Collection col = null;
		BinaryResource res = null;
		try {
			//INFO: access to path is restricted cause of permissions, 
			//TODO: check where to find the URI
			String URI = "xmldb:exist://localhost:8080/exist/xmlrpc";
			col = DatabaseManager.getCollection(URI + path);
			res = (BinaryResource)col.createResource(filename + nameAppendix, "BinaryResource");
			res.setContent(data);
			col.storeResource(res);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			//clean up resources
			if(res != null) {
				try { ((EXistResource)res).freeResources(); } catch(XMLDBException xe) {log.error(xe.getMessage(),xe);}
			}
			if(col != null) {
				try { col.close(); } catch(XMLDBException xe) {log.error(xe.getMessage(),xe);}
			}
		}
	}
	*/
}