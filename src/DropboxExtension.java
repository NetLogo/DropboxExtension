import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.nlogo.api.Argument;
import org.nlogo.api.Command;
import org.nlogo.api.Context;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.OutputDestinationJ;
import org.nlogo.api.PrimitiveManager;
import org.nlogo.api.Reporter;
import org.nlogo.app.App;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;
import org.nlogo.nvm.ExtensionContext;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.RetryException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.UploadErrorException;

public class DropboxExtension extends org.nlogo.api.DefaultClassManager {
	
	static DbxRequestConfig config;
	static DbxClientV2 client;
	static long backofftimer;
	private static final String ACCESS_TOKEN = ""; // Add access token here. You must sign up as a Dropbox developer to get one
	
		
	@Override
	public void load(PrimitiveManager pm) throws ExtensionException {
		pm.addPrimitive("upload", new Upload());
		pm.addPrimitive("unix-time", new UnixTime());
		pm.addPrimitive("backoff?", new BackOffCheck());
		pm.addPrimitive("backoff-timer", new BackOffTimer());
		config = new DbxRequestConfig("", "en_US"); // add Dropbox folder here
		client = new DbxClientV2(config, ACCESS_TOKEN);
		backofftimer = 0;
	}
	
	public static class Upload implements Command {

		public Syntax getSyntax() {
			// TODO Auto-generated method stub
			return SyntaxJ.commandSyntax(new int[]{Syntax.StringType()});
		}

		@Override
		public void perform(Argument[] arg0, Context arg1) throws ExtensionException {
			ExtensionContext extContext =(ExtensionContext)arg1;
			String modelPath = extContext.workspace().getModelDir();
			String filePath = arg0[0].getString();
			String finalPath = modelPath+ File.separator+ filePath;
			Path p = Paths.get(finalPath);
			String fileName = p.getFileName().toString();
			try (InputStream in = new FileInputStream(finalPath)) {
			    client.files().uploadBuilder("/"+fileName).uploadAndFinish(in);	
			    backofftimer = 0;
			}catch (RetryException e){
				backofftimer = e.getBackoffMillis();
			} catch (UploadErrorException e) {
				// TODO Auto-generated catch block
				printM(e.getMessage());
			} catch (DbxException e) {
				// TODO Auto-generated catch block
				printM(e.getMessage());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				printM(e.getMessage());
			}
		}
		
	}
	
	public static class UnixTime implements Reporter{

		@Override
		public Syntax getSyntax() {
			// TODO Auto-generated method stub
			return SyntaxJ.reporterSyntax(new int[] {}, Syntax.NumberType());
					
		}

		@Override
		public Object report(Argument[] arg0, Context arg1) throws ExtensionException {
			// TODO Auto-generated method stub
			Long l = System.currentTimeMillis();
			return l.doubleValue();
		}
		
	}
	public static class BackOffCheck implements Reporter{

		@Override
		public Syntax getSyntax() {
			// TODO Auto-generated method stub
			return SyntaxJ.reporterSyntax(new int[] {}, Syntax.BooleanType());
					
		}

		@Override
		public Object report(Argument[] arg0, Context arg1) throws ExtensionException {
			// TODO Auto-generated method stub
			return backofftimer > 0;
		}
		
	}
	
	public static class BackOffTimer implements Reporter{

		@Override
		public Syntax getSyntax() {
			// TODO Auto-generated method stub
			return SyntaxJ.reporterSyntax(new int[] {}, Syntax.NumberType());
					
		}

		@Override
		public Object report(Argument[] arg0, Context arg1) throws ExtensionException {
			// TODO Auto-generated method stub
			Long bt = (Long)backofftimer;
			return bt.doubleValue();
		}
		
	}
	
	public static void printM(Object s){
		
		try {
			App.app().workspace().outputObject((String)s, null, true, true, OutputDestinationJ.NORMAL());
		} catch (LogoException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}

}
