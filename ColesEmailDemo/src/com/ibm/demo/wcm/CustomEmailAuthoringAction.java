package com.ibm.demo.wcm;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Set;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.naming.InitialContext;

import com.ibm.portal.ListModel;
import com.ibm.websphere.management.AdminService;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.workplace.wcm.api.Content;
import com.ibm.workplace.wcm.api.Document;
import com.ibm.workplace.wcm.api.DocumentId;
import com.ibm.workplace.wcm.api.ImageComponent;
import com.ibm.workplace.wcm.api.TemplatedDocument;
import com.ibm.workplace.wcm.api.TextComponent;
import com.ibm.workplace.wcm.api.extensions.authoring.ActionResult;
import com.ibm.workplace.wcm.api.extensions.authoring.AuthoringAction;
import com.ibm.workplace.wcm.api.extensions.authoring.FormContext;

public class CustomEmailAuthoringAction implements AuthoringAction {

	private static final String BASE_SCHEME = "http";
	private static final String BASE_PATH = "/wps/wcm/connect/Coles+Email+Demo/Home/";
	private static Session mailSession = null;

	@Override
	public String getDescription(Locale locale) {
		return "Send as Email";
	}

	@Override
	public ListModel<Locale> getLocales() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTitle(Locale locale) {
		return "Send as Email";
	}

	@Override
	public ActionResult execute(FormContext formContext) {
		Document document = formContext.document();
		Content content = (Content) document;
		
		System.out.println("Send_as_Email button clicked for " + document.getId());
		
		// reference: http://blog.smartbear.com/how-to/how-to-send-email-with-embedded-images-using-java/

		try {
			String to = ((TextComponent) content.getComponentByReference("To")).getText();
			String subject = ((TextComponent) content.getComponentByReference("Subject")).getText();
			byte[] bannerImage = ((ImageComponent) content.getComponentByReference("Banner Image")).getImage();
			String bannerImageFilename = ((ImageComponent) content.getComponentByReference("Banner Image")).getImageFileName();
			byte[] mainImage = ((ImageComponent) content.getComponentByReference("Main Image")).getImage();
			String mainImageFilename = ((ImageComponent) content.getComponentByReference("Main Image")).getImageFileName();
			byte[] sidebarImage = ((ImageComponent) content.getComponentByReference("Sidebar Image")).getImage();
			String sidebarImageFilename = ((ImageComponent) content.getComponentByReference("Sidebar Image")).getImageFileName();
			
			HttpURLConnection connection = null;
		    URL url = new URL(BASE_SCHEME + "://" + getWcmHost() + BASE_PATH + content.getName());
		    connection = (HttpURLConnection) url.openConnection();
		    connection.setRequestMethod("GET");
		    connection.setUseCaches(false);
		    connection.setDoOutput(true);
		    
		    InputStream is = connection.getInputStream();
		    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		    StringBuilder response = new StringBuilder(); 
		    String line;
		    while ((line = rd.readLine()) != null) {
		      response.append(line);
		      response.append('\r');
		    }
		    rd.close();
			  
			MimeMessage msg = new MimeMessage(getMailSession());
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));	
			msg.setFrom(new InternetAddress("vicleung@au1.ibm.com"));
			msg.setSubject(subject);
			
			MimeMultipart multiPart = new MimeMultipart();
			multiPart.setSubType("related");
			msg.setContent(multiPart);
					
			MimeBodyPart mainPart = new MimeBodyPart();
			mainPart.setContent(response.toString(), "text/html");
			multiPart.addBodyPart(mainPart);
			
			MimeBodyPart bannerImagePart = new MimeBodyPart();
			ByteArrayDataSource bannerImageDs = new ByteArrayDataSource(bannerImage, "image/jpeg");
			bannerImagePart.setDataHandler(new DataHandler(bannerImageDs));
			bannerImagePart.setFileName(bannerImageFilename);
			bannerImagePart.setContentID("<bannerimage>");
			bannerImagePart.setDisposition(MimeBodyPart.INLINE);
			multiPart.addBodyPart(bannerImagePart);
			
			MimeBodyPart mainImagePart = new MimeBodyPart();
			ByteArrayDataSource sidebarOneImageDs = new ByteArrayDataSource(mainImage, "image/jpeg");
			mainImagePart.setDataHandler(new DataHandler(sidebarOneImageDs));
			mainImagePart.setFileName(mainImageFilename);
			mainImagePart.setContentID("<mainimage>");
			mainImagePart.setDisposition(MimeBodyPart.INLINE);
			multiPart.addBodyPart(mainImagePart);
			
			MimeBodyPart sidebarImagePart = new MimeBodyPart();
			ByteArrayDataSource sidebarTwoImageDs = new ByteArrayDataSource(sidebarImage, "image/jpeg");
			sidebarImagePart.setDataHandler(new DataHandler(sidebarTwoImageDs));
			sidebarImagePart.setFileName(sidebarImageFilename);
			sidebarImagePart.setContentID("<sidebarimage>");
			sidebarImagePart.setDisposition(MimeBodyPart.INLINE);
			multiPart.addBodyPart(sidebarImagePart);
			
			Transport.send(msg);
			
			System.out.println("Sent: Send_as_Email button for " + document.getId());


		} catch (Exception e) {
			e.printStackTrace();
			// do nothing
		}
		
		System.out.println("Done: Send_as_Email button for " + document.getId());

		return null;
	}

	@Override
	public boolean isValidForForm(FormContext formContext) {
		Document document = formContext.document();

		if (document instanceof TemplatedDocument) {
			Content content = (Content) document;

			try {
				DocumentId templateId = content.getAuthoringTemplateID();

				Document template = content.getSourceWorkspace().getById(templateId);

				if (template.getName().contains("Email")) {
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
				// do nothing
			}
		}

		return false;
	}

	@Override
	public int ordinal() {
		return 0;
	}

	public static Session getMailSession() {
		if (mailSession == null) {
			try {
				InitialContext ctx = new InitialContext();
				mailSession = (Session) ctx.lookup("mail/localmail");
			} catch (Exception e) {
				e.printStackTrace();
				// do nothing
			}
		}

		return mailSession;
	}
	
	private String getWcmHost() {
		try
		{
		    AdminService adminService = AdminServiceFactory.getAdminService();
		    ObjectName queryName = new ObjectName( "WebSphere:*,type=AdminOperations" );
		    Set objs = adminService.queryNames( queryName, null );
		    if ( !objs.isEmpty() )
		    {
		        ObjectName thisObj = (ObjectName)objs.iterator().next();
		        String opName = "expandVariable";
		        String signature[] = { "java.lang.String" };
		        String params[] = { "${WCM_HOST}" } ;
		        String retVal = (String) adminService.invoke( thisObj, opName, params, signature );
		        System.out.println( retVal );
		        return retVal;
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		} 
		
		return null;
	}

}
