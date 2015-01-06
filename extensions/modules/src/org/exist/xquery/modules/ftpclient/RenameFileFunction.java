package org.exist.xquery.modules.ftpclient;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
public class RenameFileFunction extends BasicFunction {

    private static final FunctionParameterSequenceType CONNECTION_HANDLE_PARAM = new FunctionParameterSequenceType("connection-handle", Type.LONG, Cardinality.EXACTLY_ONE, "The connection handle");
    private static final FunctionParameterSequenceType REMOTE_DIRECTORY_PARAM = new FunctionParameterSequenceType("remote-directory", Type.STRING, Cardinality.EXACTLY_ONE, "The remote directory");
    private static final FunctionParameterSequenceType FILE_NAME_FROM_PARAM = new FunctionParameterSequenceType("file-name-old", Type.STRING, Cardinality.EXACTLY_ONE, "Origin File name");
    private static final FunctionParameterSequenceType FILE_NAME_TO_PARAM = new FunctionParameterSequenceType("file-name-new", Type.STRING, Cardinality.EXACTLY_ONE, "New File name" );
    
    private static final Logger log = Logger.getLogger(RenameFileFunction.class);
    
    public final static FunctionSignature signature = new FunctionSignature(
        new QName("rename-file", FTPClientModule.NAMESPACE_URI, FTPClientModule.PREFIX),
        "Rename file via FTP.",
        new SequenceType[] {
            CONNECTION_HANDLE_PARAM,
            REMOTE_DIRECTORY_PARAM,
            FILE_NAME_FROM_PARAM,
			FILE_NAME_TO_PARAM
        },
        new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE, "true or false indicating the success of the file rename")
    );

    public RenameFileFunction(XQueryContext context) {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        
        Sequence result = Sequence.EMPTY_SEQUENCE;
        
        long connectionUID = ((IntegerValue)args[0].itemAt(0)).getLong();
        FTPClient ftp = FTPClientModule.retrieveConnection(context, connectionUID);
        if(ftp != null) {
            String remoteDirectory = args[1].getStringValue();
            String orgFileName = args[2].getStringValue();
			String newFileName = args[3].getStringValue();
            
            result = renameFile(ftp, remoteDirectory, orgFileName, newFileName);
			
			log.info("FTP server rename binary File: from " + orgFileName + " to "  +newFileName + " in dir " + remoteDirectory);
        }
        
        return result;
    }

    private Sequence renameFile(FTPClient ftp, String remoteDirectory, String orgFileName, String newFileName) {
        
        boolean result = false;
        try {
			//change working directory
            boolean cdResult = ftp.changeWorkingDirectory(remoteDirectory);
			//first check if directory is changed
			if ( cdResult ) {
				ftp.setFileType(FTP.BINARY_FILE_TYPE);
				//try fist rename by using ftp rename command
				result = renameFileSimple(ftp, orgFileName, newFileName);
				if (!result){
					//if not OK, try to copy the original file to a new file and delete the original
					result = copyDelFile(ftp, orgFileName, newFileName);
				}
				//if still not OK create error message
				if (!result) {
					log.error("FTP server unable to rename File: from " + orgFileName + " to "  +newFileName + " in dir " + remoteDirectory);
				}
			} else {
				log.error("FTP server unable to rename File: from " + orgFileName + " to "  +newFileName + " in dir " + remoteDirectory + ". Message: " + ftp.getReplyString());
			}
        } catch(IOException ioe) {
            log.error(ioe.getMessage(), ioe);
            result = false;
        }
        
        return BooleanValue.valueOf(result);
    }
	
	/**
	 * Method uses FTPClient.rename to rename orgFileName to newFileName.<br/>
	 * Be sure that FTPClient is already moved to the working directory where orgFoleName is in.
	 * @param ftp FTPClient
	 * @param orgFileName String name of the file to be renamed (no path contained)
	 * @param newFileName String name of the renamed file (no path contained)
	 * @return corresponds to the return value of FTPClient.rename method
	 * @throws IOException
	 */
	private Boolean renameFileSimple(FTPClient ftp, String orgFileName, String newFileName) throws IOException {
		boolean result = ftp.rename(orgFileName, newFileName);
		if (!result){
			String message = "FTP server rename File: "+ftp.getReplyString();
			log.warn(message);			
		}
		return Boolean.valueOf(result);
	}
	
	/**
	 * Method copies an existing file with orgFileName to a file with name newFileName using FTPClient methods retrieveFile <br/>
	 * to retrieve the origin file and FTPClient method storeUniqueFile to store it again using newFileName. <br/>
	 * After an successfully copy of the origin file it uses FTPClient method deleteFile to remove the origin file. <br/>
	 * The method stops if one intermediate steps returns false. No roll-back or clean-up is performed on error. 
	 * @param ftp FTPClient
	 * @param orgFileName String name of the file to be renamed (no path contained)
	 * @param newFileName String name of the renamed file (no path contained)
	 * @throws IOException
	 */
	private Boolean copyDelFile(FTPClient ftp, String orgFileName, String newFileName) throws IOException {
		boolean result = false;
		//get file from ftp as input
		//create new output,
		//of ok, delete old.
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		result = ftp.retrieveFile(orgFileName, outputStream);
		if (result) {
			InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
			ftp.setFileType(FTP.BINARY_FILE_TYPE);//binary files
			//copy file
			result = ftp.storeUniqueFile(newFileName, is);
			if (result) {
				//delete old one
				result = ftp.deleteFile(orgFileName);
			}
		} 
		if (!result){
			String message = "FTP server rename File: "+ftp.getReplyString();
			log.warn(message);			
		}
		return Boolean.valueOf(result);
	}
}