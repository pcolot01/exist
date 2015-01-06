package org.exist.xquery.modules.ftpclient;

import java.io.IOException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

import org.exist.dom.QName;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.BinaryValue;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.IntegerValue;

/**
 *
 * 
 */
public class RemoveDirectoryFunction extends BasicFunction {

    private static final FunctionParameterSequenceType CONNECTION_HANDLE_PARAM = new FunctionParameterSequenceType("connection-handle", Type.LONG, Cardinality.EXACTLY_ONE, "The connection handle");
    private static final FunctionParameterSequenceType REMOTE_DIRECTORY_PARAM = new FunctionParameterSequenceType("remote-directory", Type.STRING, Cardinality.EXACTLY_ONE, "The remote directory");
    private static final FunctionParameterSequenceType DIRECTORY_NAME_PARAM = new FunctionParameterSequenceType("directory-name", Type.STRING, Cardinality.EXACTLY_ONE, "Directory name to delete");
    
    private static final Logger log = Logger.getLogger(RemoveDirectoryFunction.class);
    
    public final static FunctionSignature signature = new FunctionSignature(
        new QName("remove-directory", FTPClientModule.NAMESPACE_URI, FTPClientModule.PREFIX),
        "Delete directory via FTP.",
        new SequenceType[] {
            CONNECTION_HANDLE_PARAM,
            REMOTE_DIRECTORY_PARAM,
            DIRECTORY_NAME_PARAM
        },
        new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE, "true or false indicating the success of the file deleted")
    );

    public RemoveDirectoryFunction(XQueryContext context) {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        
        Sequence result = Sequence.EMPTY_SEQUENCE;
        
        long connectionUID = ((IntegerValue)args[0].itemAt(0)).getLong();
        FTPClient ftp = FTPClientModule.retrieveConnection(context, connectionUID);
        if(ftp != null) {
            String remoteDirectory = args[1].getStringValue();
            String directoryName = args[2].getStringValue();
            
            result = removeDirectory(ftp, remoteDirectory, directoryName);
			log.warn("FTP server remove directory: " + directoryName + " from " + directoryName);
        }
        
        return result;
    }

    private Sequence removeDirectory(FTPClient ftp, String remoteDirectory, String directoryName) {
        
        boolean result = false;
        try {
            boolean cdResult = ftp.changeWorkingDirectory(remoteDirectory);
			if ( cdResult ) {
				ftp.setFileType(FTP.BINARY_FILE_TYPE);
				//try deleting remote file
				result = ftp.removeDirectory(directoryName);
				String message = "FTP server remove directory: "+directoryName+ " from " + remoteDirectory + ". Message: " + ftp.getReplyString();
				if (!result) {
					log.error(message);
				} else {
					log.info(message);
				}
			} else {
				log.error("FTP server remove directory: "+directoryName+ " from " + remoteDirectory + ". Message: " + ftp.getReplyString());
			}
        } catch(IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            result = false;
        }
        
        return BooleanValue.valueOf(result);
    }
}