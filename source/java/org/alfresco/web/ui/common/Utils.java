/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.web.ui.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletContext;

import org.alfresco.config.ConfigElement;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.filesys.CIFSServerBean;
import org.alfresco.filesys.repo.ContentContext;
import org.alfresco.jlan.server.core.SharedDevice;
import org.alfresco.jlan.server.core.SharedDeviceList;
import org.alfresco.jlan.server.filesys.DiskSharedDevice;
import org.alfresco.jlan.server.filesys.FilesystemsConfigSection;
import org.alfresco.model.ApplicationModel;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.webdav.WebDAVServlet;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.FileTypeImageSize;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NoTransformerException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.DownloadContentServlet;
import org.alfresco.web.app.servlet.ExternalAccessServlet;
import org.alfresco.web.bean.NavigationBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.data.IDataContainer;
import org.alfresco.web.ui.common.component.UIStatusMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.myfaces.shared_impl.renderkit.html.HtmlFormRendererBase;
import org.springframework.util.StringUtils;
import org.springframework.web.jsf.FacesContextUtils;

/**
 * Class containing misc helper methods used by the JSF components.
 * 
 * @author Kevin Roast
 */
public final class Utils
{
   private static final String MSG_TIME_PATTERN = "time_pattern";
   private static final String MSG_DATE_PATTERN = "date_pattern";
   private static final String MSG_DATE_TIME_PATTERN = "date_time_pattern";
   
   private static final String IMAGE_PREFIX16 = "/images/filetypes/";
   private static final String IMAGE_PREFIX32 = "/images/filetypes32/";
   private static final String IMAGE_PREFIX64 = "/images/filetypes64/";
   private static final String IMAGE_POSTFIX_GIF = ".gif";
   private static final String IMAGE_POSTFIX_PNG = ".png";
   private static final String DEFAULT_FILE_IMAGE16 = IMAGE_PREFIX16 + "_default" + IMAGE_POSTFIX_GIF;
   private static final String DEFAULT_FILE_IMAGE32 = IMAGE_PREFIX32 + "_default" + IMAGE_POSTFIX_GIF;
   private static final String DEFAULT_FILE_IMAGE64 = IMAGE_PREFIX64 + "_default" + IMAGE_POSTFIX_PNG;
   
   private static final Map<String, String> s_fileExtensionMap = new HashMap<String, String>(89, 1.0f);
   
   private static final Log logger = LogFactory.getLog(Utils.class);
   
   private static final Set<String> safeTags = new HashSet<String>();
   
   static
   {
      safeTags.add("p");
      safeTags.add("/p");
      safeTags.add("b");
      safeTags.add("/b");
      safeTags.add("i");
      safeTags.add("/i");
      safeTags.add("br");
      safeTags.add("ul");
      safeTags.add("/ul");
      safeTags.add("ol");
      safeTags.add("/ol");
      safeTags.add("li");
      safeTags.add("/li");
      safeTags.add("h1");
      safeTags.add("/h1");
      safeTags.add("h2");
      safeTags.add("/h2");
      safeTags.add("h3");
      safeTags.add("/h3");
      safeTags.add("h4");
      safeTags.add("/h4");
      safeTags.add("h5");
      safeTags.add("/h5");
      safeTags.add("h6");
      safeTags.add("/h6");
      safeTags.add("span");
      safeTags.add("/span");
      safeTags.add("a");
      safeTags.add("/a");
      safeTags.add("img");
      safeTags.add("font");
      safeTags.add("/font");
   }
   
   /**
    * Private constructor
    */
   private Utils()
   {
   }
   
   /**
    * Encodes the given string, so that it can be used within an HTML page.
    * 
    * @param string     the String to convert
    */
   public static String encode(String string)
   {
      if (string == null)
      {
         return "";
      }

      StringBuilder sb = null;      //create on demand
      String enc;
      char c;
      for (int i = 0; i < string.length(); i++)
      {
         enc = null;
         c = string.charAt(i);
         switch (c)
         {
            case '"': enc = "&quot;"; break;    //"
            case '&': enc = "&amp;"; break;     //&
            case '<': enc = "&lt;"; break;      //<
            case '>': enc = "&gt;"; break;      //>
             
            //german umlauts
            case '\u00E4' : enc = "&auml;";  break;
            case '\u00C4' : enc = "&Auml;";  break;
            case '\u00F6' : enc = "&ouml;";  break;
            case '\u00D6' : enc = "&Ouml;";  break;
            case '\u00FC' : enc = "&uuml;";  break;
            case '\u00DC' : enc = "&Uuml;";  break;
            case '\u00DF' : enc = "&szlig;"; break;
            
            //misc
            //case 0x80: enc = "&euro;"; break;  sometimes euro symbol is ascii 128, should we suport it?
            case '\u20AC': enc = "&euro;";  break;
            case '\u00AB': enc = "&laquo;"; break;
            case '\u00BB': enc = "&raquo;"; break;
            case '\u00A0': enc = "&nbsp;"; break;
            
            default:
               if (((int)c) >= 0x80)
               {
                  //encode all non basic latin characters
                  enc = "&#" + ((int)c) + ";";
               }
               break;
         }
         
         if (enc != null)
         {
            if (sb == null)
            {
               String soFar = string.substring(0, i);
               sb = new StringBuilder(i + 8);
               sb.append(soFar);
            }
            sb.append(enc);
         }
         else
         {
            if (sb != null)
            {
               sb.append(c);
            }
         }
      }
      
      if (sb == null)
      {
         return string;
      }
      else
      {
         return sb.toString();
      }
   }
   
   /**
    * Crop a label within a SPAN element, using ellipses '...' at the end of label and
    * and encode the result for HTML output. A SPAN will only be generated if the label
    * is beyond the default setting of 32 characters in length.
    * 
    * @param text       to crop and encode
    * 
    * @return encoded and cropped resulting label HTML
    */
   public static String cropEncode(String text)
   {
      return cropEncode(text, 32);
   }
   
   /**
    * Crop a label within a SPAN element, using ellipses '...' at the end of label and
    * and encode the result for HTML output. A SPAN will only be generated if the label
    * is beyond the specified number of characters in length.
    * 
    * @param text       to crop and encode
    * @param length     length of string to crop too
    * 
    * @return encoded and cropped resulting label HTML
    */
   public static String cropEncode(String text, int length)
   {
      if (text.length() > length)
      {
         String label = text.substring(0, length - 3) + "...";
         StringBuilder buf = new StringBuilder(length + 32 + text.length());
         buf.append("<span title=\"")
            .append(Utils.encode(text))
            .append("\">")
            .append(Utils.encode(label))
            .append("</span>");
         return buf.toString();
      }
      else
      {
         return Utils.encode(text);
      }
   }
   
   /**
    * Encode a string to the %AB hex style JavaScript compatible notation.
    * Used to encode a string to a value that can be safely inserted into an HTML page and
    * then decoded (and probably eval()ed) using the unescape() JavaScript method.
    * 
    * @param s      string to encode
    * 
    * @return %AB hex style encoded string
    */
   public static String encodeJavascript(String s)
   {
       StringBuilder buf = new StringBuilder(s.length() * 3);
       for (int i=0; i<s.length(); i++)
       {
           char c = s.charAt(i);
           int iChar = (int)c;
           buf.append('%');
           buf.append(Integer.toHexString(iChar));
       }
       return buf.toString();
   }
   
   /**
    * Strip unsafe HTML tags from a string - only leaves most basic formatting tags
    * and encodes or strips the remaining characters.
    * 
    * @param s HTML string to strip tags from
    * 
    * @return safe string
    */
   public static String stripUnsafeHTMLTags(String s)
   {
      s = s.replace("onclick", "$");
      s = s.replace("onmouseover", "$");
      s = s.replace("onmouseout", "$");
      s = s.replace("onmousemove", "$");
      s = s.replace("onfocus", "$");
      s = s.replace("onblur", "$");
      StringBuilder buf = new StringBuilder(s.length());
      char[] chars = s.toCharArray();
      for (int i=0; i<chars.length; i++)
      {
         if (chars[i] == '<')
         {
            // found a tag?
            int endMatchIndex = -1;
            int endTagIndex = -1;
            if (i < chars.length - 2)
            {
               for (int x=(i + 1); x<chars.length; x++)
               {
                  if (chars[x] == ' ' && endMatchIndex == -1)
                  {
                     // keep track of the match point for comparing tags in the safeTags set
                     endMatchIndex = x;
                  }
                  else if (chars[x] == '>')
                  {
                     endTagIndex = x;
                     break;
                  }
                  else if (chars[x] == '<')
                  {
                     // found another angle bracket - not a tag def so we can safely output to here
                     break;
                  }
               }
            }
            if (endTagIndex != -1)
            {
               // found end of the tag to match
               String tag = s.substring(i + 1, endTagIndex).toLowerCase();
               String matchTag = tag;
               if (endMatchIndex != -1)
               {
                  matchTag = s.substring(i + 1, endMatchIndex).toLowerCase();
               }
               if (safeTags.contains(matchTag))
               {
                  // safe tag - append to buffer
                  buf.append('<').append(tag).append('>');
               }
               // inc counter to skip past whole tag
               i = endTagIndex;
               continue;
            }
         }
         String enc = null;
         switch (chars[i])
         {
            case '"': enc = "&quot;"; break;
            case '&': enc = "&amp;"; break;
            case '<': enc = "&lt;"; break;
            case '>': enc = "&gt;"; break;
            
            default:
               if (((int)chars[i]) >= 0x80)
               {
                  //encode all non basic latin characters
                  enc = "&#" + ((int)chars[i]) + ";";
               }
            break;
         }
         buf.append(enc == null ? chars[i] : enc);
      }
      return buf.toString();
   }
   
   /**
    * Replace one string instance with another within the specified string
    * 
    * @param str
    * @param repl
    * @param with
    * 
    * @return replaced string
    */
   public static String replace(String str, String repl, String with)
   {
       int lastindex = 0;
       int pos = str.indexOf(repl);

       // If no replacement needed, return the original string
       // and save StringBuffer allocation/char copying
       if (pos < 0)
       {
           return str;
       }
       
       int len = repl.length();
       int lendiff = with.length() - repl.length();
       StringBuilder out = new StringBuilder((lendiff <= 0) ? str.length() : (str.length() + (lendiff << 3)));
       for (; pos >= 0; pos = str.indexOf(repl, lastindex = pos + len))
       {
           out.append(str.substring(lastindex, pos)).append(with);
       }
       
       return out.append(str.substring(lastindex, str.length())).toString();
   }
   
   /**
    * Remove all occurances of a String from a String
    * 
    * @param str     String to remove occurances from
    * @param match   The string to remove
    * 
    * @return new String with occurances of the match removed
    */
   public static String remove(String str, String match)
   {
      int lastindex = 0;
      int pos = str.indexOf(match);

      // If no replacement needed, return the original string
      // and save StringBuffer allocation/char copying
      if (pos < 0)
      {
          return str;
      }
      
      int len = match.length();
      StringBuilder out = new StringBuilder(str.length());
      for (; pos >= 0; pos = str.indexOf(match, lastindex = pos + len))
      {
          out.append(str.substring(lastindex, pos));
      }
      
      return out.append(str.substring(lastindex, str.length())).toString();
   }
   
   /**
    * Replaces carriage returns and line breaks with the &lt;br&gt; tag.
    * 
    * @param str The string to be parsed
    * @return The string with line breaks removed
    */
   public static String replaceLineBreaks(String str)
   {
      String replaced = null;
      
      if (str != null)
      {
         try
         {
            StringBuilder parsedContent = new StringBuilder(str.length() + 32);
            BufferedReader reader = new BufferedReader(new StringReader(str));
            String line = reader.readLine();
            while (line != null)
            {
               parsedContent.append(line);
               line = reader.readLine();
               if (line != null)
               {
                  parsedContent.append("<br>");
               }
            }
            
            replaced = parsedContent.toString();
         }
         catch (IOException ioe)
         {
            if (logger.isWarnEnabled())
            {
               logger.warn("Failed to replace line breaks in string: " + str);
            }
         }
      }
      
      return replaced;
   }
   
   /**
    * Helper to output an attribute to the output stream
    * 
    * @param out        ResponseWriter
    * @param attr       attribute value object (cannot be null)
    * @param mapping    mapping to output as e.g. style="..."
    * 
    * @throws IOException
    */
   public static void outputAttribute(ResponseWriter out, Object attr, String mapping)
      throws IOException
   {
      if (attr != null)
      {
         out.write(' ');
         out.write(mapping);
         out.write("=\"");
         out.write(attr.toString());
         out.write('"');
      }
   }
   
   /**
    * Get the hidden field name for any action component.
    * 
    * All components that wish to simply encode a form value with their client ID can reuse the same
    * hidden field within the parent form. NOTE: components which use this method must only encode
    * their client ID as the value and nothing else!
    * 
    * Build a shared field name from the parent form name and the string "act".
    * 
    * @return hidden field name shared by all action components within the Form.
    */
   public static String getActionHiddenFieldName(FacesContext context, UIComponent component)
   {
      return Utils.getParentForm(context, component).getClientId(context) + NamingContainer.SEPARATOR_CHAR + "act";
   }
   
   /**
    * Helper to recursively render a component and it's child components
    * 
    * @param context    FacesContext
    * @param component  UIComponent
    * 
    * @throws IOException
    */
   public static void encodeRecursive(FacesContext context, UIComponent component)
      throws IOException
   {
      if (component.isRendered() == true)
      {
         component.encodeBegin(context);
         
         // follow the spec for components that render their children
         if (component.getRendersChildren() == true)
         {
            component.encodeChildren(context);
         }
         else
         {
            if (component.getChildCount() != 0)
            {
               for (Iterator i=component.getChildren().iterator(); i.hasNext(); /**/)
               {
                  encodeRecursive(context, (UIComponent)i.next());
               }
            }
         }
         
         component.encodeEnd(context);
      }
   }
   
   /**
    * Generate the JavaScript to submit set the specified hidden Form field to the
    * supplied value and submit the parent Form.
    * 
    * NOTE: the supplied hidden field name is added to the Form Renderer map for output.
    * 
    * @param context       FacesContext
    * @param component     UIComponent to generate JavaScript for
    * @param fieldId       Hidden field id to set value for
    * @param fieldValue    Hidden field value to set hidden field too on submit
    * 
    * @return JavaScript event code
    */
   public static String generateFormSubmit(FacesContext context, UIComponent component, String fieldId, String fieldValue)
   {
      return generateFormSubmit(context, component, fieldId, fieldValue, false, null);
   }
   
   /**
    * Generate the JavaScript to submit set the specified hidden Form field to the
    * supplied value and submit the parent Form.
    * 
    * NOTE: the supplied hidden field name is added to the Form Renderer map for output.
    * 
    * @param context       FacesContext
    * @param component     UIComponent to generate JavaScript for
    * @param fieldId       Hidden field id to set value for
    * @param fieldValue    Hidden field value to set hidden field too on submit
    * @param params        Optional map of param name/values to output
    * 
    * @return JavaScript event code
    */
   public static String generateFormSubmit(FacesContext context, UIComponent component, String fieldId, 
         String fieldValue, Map<String, String> params)
   {
      return generateFormSubmit(context, component, fieldId, fieldValue, false, params);
   }
      
   /**
    * Generate the JavaScript to submit set the specified hidden Form field to the
    * supplied value and submit the parent Form.
    * 
    * NOTE: the supplied hidden field name is added to the Form Renderer map for output.
    * 
    * @param context       FacesContext
    * @param component     UIComponent to generate JavaScript for
    * @param fieldId       Hidden field id to set value for
    * @param fieldValue    Hidden field value to set hidden field too on submit
    * @param valueIsParam  Determines whether the fieldValue parameter should be treated
    *                      as a parameter in the generated JavaScript, false will treat
    *                      the value i.e. surround it with single quotes
    * @param params        Optional map of param name/values to output
    * 
    * @return JavaScript event code
    */
   public static String generateFormSubmit(FacesContext context, UIComponent component, String fieldId, 
         String fieldValue, boolean valueIsParam, Map<String, String> params)
   {
      UIForm form = Utils.getParentForm(context, component);
      if (form == null)
      {
         throw new IllegalStateException("Must nest components inside UIForm to generate form submit!");
      }
      
      String formClientId = form.getClientId(context);
      
      StringBuilder buf = new StringBuilder(200);
      buf.append("document.forms['");
      buf.append(formClientId);
      buf.append("']['");
      buf.append(fieldId);
      buf.append("'].value=");
      if (valueIsParam == false)
      {
         buf.append("'");
      }
      buf.append(fieldValue);
      if (valueIsParam == false)
      {
         buf.append("'");
      }
      buf.append(";");
      
      if (params != null)
      {
         for (String name : params.keySet())
         {
            buf.append("document.forms['");
            buf.append(formClientId);
            buf.append("']['");
            buf.append(name);
            buf.append("'].value='");
            buf.append(StringUtils.replace(params.get(name), "'", "\\'"));
            buf.append("';");
            
            // weak, but this seems to be the way Sun RI do it...
            //FormRenderer.addNeededHiddenField(context, name);
            HtmlFormRendererBase.addHiddenCommandParameter(context, form, name);
         }
      }
      
      buf.append("document.forms['");
      buf.append(formClientId);
      buf.append("'].submit();");
      
      if (valueIsParam == false)
      {
         buf.append("return false;");
      }
      
      // weak, but this seems to be the way Sun RI do it...
      //FormRenderer.addNeededHiddenField(context, fieldId);
      HtmlFormRendererBase.addHiddenCommandParameter(context, form, fieldId);
      
      return buf.toString();
   }
   
   /**
    * Generate the JavaScript to submit the parent Form.
    * 
    * @param context       FacesContext
    * @param component     UIComponent to generate JavaScript for
    * 
    * @return JavaScript event code
    */
   public static String generateFormSubmit(FacesContext context, UIComponent component)
   {
      UIForm form = Utils.getParentForm(context, component);
      if (form == null)
      {
         throw new IllegalStateException("Must nest components inside UIForm to generate form submit!");
      }
      
      String formClientId = form.getClientId(context);
      
      StringBuilder buf = new StringBuilder(48);
      
      buf.append("document.forms['");
      buf.append(formClientId);
      buf.append("'].submit()");
      
      buf.append(";return false;");
      
      return buf.toString();
   }
   
   /**
    * Enum representing the client URL type to generate
    */
   public enum URLMode {HTTP_DOWNLOAD, HTTP_INLINE, WEBDAV, CIFS, SHOW_DETAILS, BROWSE, FTP}
   
   /**
    * Generates a URL for the given usage for the given node.
    * 
    * The supported values for the usage parameter are of URLMode enum type
    * @see URLMode
    * 
    * @param context    Faces context
    * @param node       The node to generate the URL for
    * @param name       Name to use for the download file part of the link if any
    * @param usage      What the URL is going to be used for
    * 
    * @return The URL for the requested usage without the context path
    */
   public static String generateURL(FacesContext context, Node node, String name, URLMode usage)
   {
      String url = null;
      
      switch (usage)
      {
         case WEBDAV:
         {
            // calculate a WebDAV URL for the given node
            FileFolderService fileFolderService = Repository.getServiceRegistry(
                  context).getFileFolderService();
            try
            {
               List<FileInfo> paths = fileFolderService.getNamePath(null, node.getNodeRef());
               
               // build up the webdav url
               StringBuilder path = new StringBuilder("/").append(WebDAVServlet.WEBDAV_PREFIX);
               
               // build up the path skipping the first path as it is the root folder
               for (int x = 1; x < paths.size(); x++)
               {
                  path.append("/").append(URLEncoder.encode(paths.get(x).getName()));
               }
               url = path.toString();
            }
            catch (AccessDeniedException e)
            {
               // cannot build path if user don't have access all the way up
            }
            catch (FileNotFoundException nodeErr)
            {
               // cannot build path if file no longer exists
            }
            break;
         }
         
         case CIFS:
         {
            // calculate a CIFS path for the given node
            
            // get hold of the node service, cifsServer and navigation bean
            NodeService nodeService = Repository.getServiceRegistry(context).getNodeService();
            NavigationBean navBean = (NavigationBean)context.getExternalContext().
                  getSessionMap().get(NavigationBean.BEAN_NAME);
            CIFSServerBean cifsServer = (CIFSServerBean)FacesContextUtils.getRequiredWebApplicationContext(
                  context).getBean("cifsServer");
            
            if (nodeService != null && navBean != null && cifsServer != null)
            {
                // Resolve CIFS network folder location for this node
                
                FilesystemsConfigSection filesysConfig = (FilesystemsConfigSection) cifsServer.getConfiguration().getConfigSection(FilesystemsConfigSection.SectionName); 
                DiskSharedDevice diskShare = null;
                 
                SharedDeviceList shares = filesysConfig.getShares();
                Enumeration<SharedDevice> shareEnum = shares.enumerateShares();
                 
                while ( shareEnum.hasMoreElements() && diskShare == null) {
                   SharedDevice curShare = shareEnum.nextElement();
                   if ( curShare.getContext() instanceof ContentContext)
                     diskShare = (DiskSharedDevice) curShare;
                }
               
               
               if (diskShare != null)
               {
                  ContentContext contentCtx = (ContentContext) diskShare.getContext();
                  NodeRef rootNode = contentCtx.getRootNode();
                  try
                  {
                     url = Repository.getNamePath(nodeService, node.getNodePath(), rootNode, "\\", 
                           "file:///" + navBean.getCIFSServerPath(diskShare));
                  }
                  catch (AccessDeniedException e)
                  {
                     // cannot build path if user don't have access all the way up
                  }
                  catch (InvalidNodeRefException nodeErr)
                  {
                     // cannot build path if node no longer exists
                  }
               }
            }
            break;
         }
         
         case HTTP_DOWNLOAD:
         {
            url = DownloadContentServlet.generateDownloadURL(node.getNodeRef(), name);
            break;
         }
         
         case HTTP_INLINE:
         {
            url = DownloadContentServlet.generateBrowserURL(node.getNodeRef(), name);
            break;
         }
         
         case SHOW_DETAILS:
         {
            DictionaryService dd = Repository.getServiceRegistry(context).getDictionaryService();
            
            // default to showing details of content
            String outcome = ExternalAccessServlet.OUTCOME_DOCDETAILS;
            
            // if the node is a type of folder then make the outcome to show space details
            if ((dd.isSubClass(node.getType(), ContentModel.TYPE_FOLDER)) ||
                  (dd.isSubClass(node.getType(), ApplicationModel.TYPE_FOLDERLINK)))
            {
               outcome = ExternalAccessServlet.OUTCOME_SPACEDETAILS;
            }
            
            // build the url
            url = ExternalAccessServlet.generateExternalURL(outcome, 
                  Repository.getStoreRef().getProtocol() + "/" + 
                  Repository.getStoreRef().getIdentifier() + "/" + node.getId());
            break;
         }
         
         case BROWSE:
         {
            url = ExternalAccessServlet.generateExternalURL(ExternalAccessServlet.OUTCOME_BROWSE, 
                  Repository.getStoreRef().getProtocol() + "/" + 
                  Repository.getStoreRef().getIdentifier() + "/" + node.getId());
         }
         
         case FTP:
         {
            // not implemented yet!
            break;
         }
      }
      
      return url;
   }
   
   /**
    * Generates a URL for the given usage for the given node.
    * 
    * The supported values for the usage parameter are of URLMode enum type
    * @see URLMode
    * 
    * @param context    Faces context
    * @param node       The node to generate the URL for
    * @param usage      What the URL is going to be used for
    * 
    * @return The URL for the requested usage without the context path
    */
   public static String generateURL(FacesContext context, Node node, URLMode usage)
   {
      return generateURL(context, node, node.getName(), usage);
   }
   
   /**
    * Build a context path safe image tag for the supplied image path.
    * Image path should be supplied with a leading slash '/'.
    * 
    * @param context       FacesContext
    * @param image         The local image path from the web folder with leading slash '/'
    * @param width         Width in pixels
    * @param height        Height in pixels
    * @param alt           Optional alt/title text
    * @param onclick       JavaScript onclick event handler code
    * 
    * @return Populated <code>img</code> tag
    */
   public static String buildImageTag(FacesContext context, String image, int width, int height,
         String alt, String onclick)
   {
      return buildImageTag(context, image, width, height, alt, onclick, null);
   }
   
   /**
    * Build a context path safe image tag for the supplied image path.
    * Image path should be supplied with a leading slash '/'.
    * 
    * @param context       FacesContext
    * @param image         The local image path from the web folder with leading slash '/'
    * @param width         Width in pixels
    * @param height        Height in pixels
    * @param alt           Optional alt/title text
    * @param onclick       JavaScript onclick event handler code
    * @param verticalAlign         Optional HTML alignment value
    * 
    * @return Populated <code>img</code> tag
    */
   public static String buildImageTag(FacesContext context, String image, int width, int height,
                                      String alt, String onclick, String verticalAlign)
   {
      StringBuilder buf = new StringBuilder(200);
      
      String style = "border-width:0px;";
      buf.append("<img src='")
         .append(context.getExternalContext().getRequestContextPath())
         .append(image)
         .append("' width='")
         .append(width)
         .append("' height='")
         .append(height)
         .append("'");
      
      if (alt != null)
      {
         alt = Utils.encode(alt);
         buf.append(" alt='")
            .append(alt)
            .append("' title='")
            .append(alt)
            .append("'");
      }
      else
      {
         buf.append(" alt=''");
      }

      if (verticalAlign != null)
      {
         StringBuilder styleBuf = new StringBuilder(40);
         styleBuf.append(style).append("vertical-align:").append(verticalAlign).append(";");
         style = styleBuf.toString();
      }
      
      if (onclick != null)
      {
         buf.append(" onclick=\"").append(onclick).append('"');
         StringBuilder styleBuf = new StringBuilder(style.length() + 16);
         styleBuf.append(style).append("cursor:pointer;");
         style = styleBuf.toString();
      }
      buf.append(" style='").append(style).append("'/>");
      
      return buf.toString();
   }
   
   /**
    * Build a context path safe image tag for the supplied image path.
    * Image path should be supplied with a leading slash '/'.
    * 
    * @param context       FacesContext
    * @param image         The local image path from the web folder with leading slash '/'
    * @param width         Width in pixels
    * @param height        Height in pixels
    * @param alt           Optional alt/title text
    * 
    * @return Populated <code>img</code> tag
    */
   public static String buildImageTag(FacesContext context, String image, int width, int height, String alt)
   {
      return buildImageTag(context, image, width, height, alt, null);
   }
   
   /**
    * Build a context path safe image tag for the supplied image path.
    * Image path should be supplied with a leading slash '/'.
    * 
    * @param context       FacesContext
    * @param image         The local image path from the web folder with leading slash '/'
    * @param alt           Optional alt/title text
    * 
    * @return Populated <code>img</code> tag
    */
   public static String buildImageTag(FacesContext context, String image, String alt)
   {
      return buildImageTag(context, image, alt, null);
   }
   
   /**
    * Build a context path safe image tag for the supplied image path.
    * Image path should be supplied with a leading slash '/'.
    * 
    * @param context       FacesContext
    * @param image         The local image path from the web folder with leading slash '/'
    * @param alt           Optional alt/title text
    * @param verticalAlign         Optional HTML alignment value
    * 
    * @return Populated <code>img</code> tag
    */
   public static String buildImageTag(FacesContext context, String image, String alt, String verticalAlign)
   {
      StringBuilder buf = new StringBuilder(128);
      buf.append("<img src='")
         .append(context.getExternalContext().getRequestContextPath())
         .append(image)
         .append("' ");

      String style = "border-width:0px;";
      if (alt != null)
      {
         alt = Utils.encode(alt);
         buf.append(" alt='")
            .append(alt)
            .append("' title='")
            .append(alt)
            .append("'");
      }
      else
      {
         buf.append(" alt=''");
      }

      if (verticalAlign != null)
      {
         StringBuilder styleBuf = new StringBuilder(40);
         styleBuf.append(style).append("vertical-align:").append(verticalAlign).append(";");
         style = styleBuf.toString();
      }
      
      buf.append(" style='").append(style).append("'/>");
      
      return buf.toString();
   }
   
   /**
    * Return the parent UIForm component for the specified UIComponent
    * 
    * @param context       FaceContext
    * @param component     The UIComponent to find parent Form for
    * 
    * @return UIForm parent or null if none found in hiearachy
    */
   public static UIForm getParentForm(FacesContext context, UIComponent component)
   {
      UIComponent parent = component.getParent();
      while (parent != null)
      {
         if (parent instanceof UIForm)
         {
            break;
         }
         parent = parent.getParent();
      }
      return (UIForm)parent;
   }
   
   /**
    * Return the parent UIComponent implementing the NamingContainer interface for
    * the specified UIComponent.
    * 
    * @param context       FaceContext
    * @param component     The UIComponent to find parent Form for
    * 
    * @return NamingContainer parent or null if none found in hiearachy
    */
   public static UIComponent getParentNamingContainer(FacesContext context, UIComponent component)
   {
      UIComponent parent = component.getParent();
      while (parent != null)
      {
         if (parent instanceof NamingContainer)
         {
            break;
         }
         parent = parent.getParent();
      }
      return (UIComponent)parent;
   }
   
   /**
    * Return the parent UIComponent implementing the IDataContainer interface for
    * the specified UIComponent.
    * 
    * @param context       FaceContext
    * @param component     The UIComponent to find parent IDataContainer for
    * 
    * @return IDataContainer parent or null if none found in hiearachy
    */
   public static IDataContainer getParentDataContainer(FacesContext context, UIComponent component)
   {
      UIComponent parent = component.getParent();
      while (parent != null)
      {
         if (parent instanceof IDataContainer)
         {
            break;
         }
         parent = parent.getParent();
      }
      return (IDataContainer)parent;
   }
   
   /**
    * Determines whether the given component is disabled or readonly
    * 
    * @param component The component to test
    * @return true if the component is either disabled or set to readonly
    */
   public static boolean isComponentDisabledOrReadOnly(UIComponent component)
   {
      boolean disabled = false;
      boolean readOnly = false;
      
      Object disabledAttr = component.getAttributes().get("disabled");
      if (disabledAttr != null)
      {
         disabled = disabledAttr.equals(Boolean.TRUE);
      }
      
      if (disabled == false)
      {
         Object readOnlyAttr = component.getAttributes().get("readonly");
         if (readOnlyAttr != null)
         {
            readOnly = readOnlyAttr.equals(Boolean.TRUE);
         }
      }

      return disabled || readOnly;
   }
   
   /**
    * Invoke the method encapsulated by the supplied MethodBinding
    * 
    * @param context    FacesContext
    * @param method     MethodBinding to invoke
    * @param event      ActionEvent to pass to the method of signature:
    *                   public void myMethodName(ActionEvent event)
    */
   public static void processActionMethod(FacesContext context, MethodBinding method, ActionEvent event)
   {
      try
      {
         method.invoke(context, new Object[] {event});
      }
      catch (EvaluationException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof AbortProcessingException)
         {
            throw (AbortProcessingException)cause;
         }
         else
         {
            throw e;
         }
      }   
   }
   
   /**
    * Adds a global error message
    * 
    * @param msg        The error message
    */
   public static void addErrorMessage(String msg)
   {
      addErrorMessage(msg, null);
   }
   
   /**
    * Adds a global error message and logs exception details
    * 
    * @param msg        The error message
    * @param err        The exception to log
    */
   public static void addErrorMessage(String msg, Throwable err)
   {
      FacesContext context = FacesContext.getCurrentInstance( );
      FacesMessage facesMsg = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg);
      context.addMessage(null, facesMsg);
      if (err != null)
      {
         if ((err instanceof InvalidNodeRefException == false &&
              err instanceof AccessDeniedException == false &&
              err instanceof NoTransformerException == false) || logger.isDebugEnabled())
         {
            logger.error(msg, err);
         }
      }
   }
   
   /**
    * Adds a global status message that will be displayed by a Status Message UI component
    * 
    * @param severity   Severity of the message
    * @param msg        Text of the message
    */
   public static void addStatusMessage(FacesMessage.Severity severity, String msg)
   {
      FacesContext fc = FacesContext.getCurrentInstance();
      String time = getTimeFormat(fc).format(new Date(System.currentTimeMillis()));
      FacesMessage fm = new FacesMessage(severity, time, msg);
      fc.addMessage(UIStatusMessage.STATUS_MESSAGE, fm);
   }
   
   /**
    * @return the formatter for locale sensitive Time formatting
    */
   public static DateFormat getTimeFormat(FacesContext fc)
   {
      return getDateFormatFromPattern(fc, Application.getMessage(fc, MSG_TIME_PATTERN));
   }
   
   /**
    * @return the formatter for locale sensitive Date formatting
    */
   public static DateFormat getDateFormat(FacesContext fc)
   {
      return getDateFormatFromPattern(fc, Application.getMessage(fc, MSG_DATE_PATTERN));
   }
   
   /**
    * @return the formatter for locale sensitive Date & Time formatting
    */
   public static DateFormat getDateTimeFormat(FacesContext fc)
   {
      return getDateFormatFromPattern(fc, Application.getMessage(fc, MSG_DATE_TIME_PATTERN));
   }
   
   /**
    * @return DataFormat object for the specified pattern
    */
   private static DateFormat getDateFormatFromPattern(FacesContext fc, String pattern)
   {
      if (pattern == null)
      {
         throw new IllegalArgumentException("DateTime pattern is mandatory.");
      }
      try
      {
         return new SimpleDateFormat(pattern, Application.getLanguage(fc));
      }
      catch (IllegalArgumentException err)
      {
         throw new AlfrescoRuntimeException("Invalid DateTime pattern", err);
      }
   }
   
   /**
    * Parse XML format date YYYY-MM-DDTHH:MM:SS
    * @param isoDate
    * @return Date or null if failed to parse
    */
   public static Date parseXMLDateFormat(String isoDate)
   {
      Date parsed = null;
      
      try
      {
         int offset = 0;
         
         // extract year
         int year = Integer.parseInt(isoDate.substring(offset, offset += 4));
         if (isoDate.charAt(offset) != '-')
         {
            throw new IndexOutOfBoundsException("Expected - character but found " + isoDate.charAt(offset));
         }
         
         // extract month
         int month = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));
         if (isoDate.charAt(offset) != '-')
         {
            throw new IndexOutOfBoundsException("Expected - character but found " + isoDate.charAt(offset));
         }
         
         // extract day
         int day = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));
         if (isoDate.charAt(offset) != 'T')
         {
            throw new IndexOutOfBoundsException("Expected T character but found " + isoDate.charAt(offset));
         }
         
         // extract hours, minutes, seconds and milliseconds
         int hour = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));
         if (isoDate.charAt(offset) != ':')
         {
            throw new IndexOutOfBoundsException("Expected : character but found " + isoDate.charAt(offset));
         }
         int minutes = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));
         if (isoDate.charAt(offset) != ':')
         {
            throw new IndexOutOfBoundsException("Expected : character but found " + isoDate.charAt(offset));
         }
         int seconds = Integer.parseInt(isoDate.substring(offset += 1 , offset += 2));
         
         // initialize Calendar object
         Calendar calendar = Calendar.getInstance();
         calendar.setLenient(false);
         calendar.set(Calendar.YEAR, year);
         calendar.set(Calendar.MONTH, month - 1);
         calendar.set(Calendar.DAY_OF_MONTH, day);
         calendar.set(Calendar.HOUR_OF_DAY, hour);
         calendar.set(Calendar.MINUTE, minutes);
         calendar.set(Calendar.SECOND, seconds);
         
         // extract the date
         parsed = calendar.getTime();
      }
      catch(IndexOutOfBoundsException e)
      {
      }
      catch(NumberFormatException e)
      {
      }
      catch(IllegalArgumentException e)
      {
      }
      
      return parsed;
   }

   /**
    * Return the image path to the filetype icon for the specified file name string
    * 
    * @param name       File name to build filetype icon path for
    * @param small      True for the small 16x16 icon or false for the large 32x32 
    * 
    * @return the image path for the specified node type or the default icon if not found
    */
   public static String getFileTypeImage(String name, boolean small)
   {
      return getFileTypeImage(FacesContext.getCurrentInstance(), null, name,
             (small ? FileTypeImageSize.Small : FileTypeImageSize.Medium));
   }
   
   /**
    * Return the image path to the filetype icon for the specified file name string
    * 
    * @param fc         FacesContext
    * @param name       File name to build filetype icon path for
    * @param small      True for the small 16x16 icon or false for the large 32x32 
    * 
    * @return the image path for the specified node type or the default icon if not found
    */
   public static String getFileTypeImage(FacesContext fc, String name, boolean small)
   {
      return getFileTypeImage(fc, null, name, (small ? FileTypeImageSize.Small : FileTypeImageSize.Medium));
   }
   
   /**
    * Return the image path to the filetype icon for the specified file name string
    * 
    * @param fc         FacesContext
    * @param name       File name to build filetype icon path for
    * @param size       Size of the icon to return
    * 
    * @return the image path for the specified node type or the default icon if not found
    */
   public static String getFileTypeImage(FacesContext fc, String name, FileTypeImageSize size)
   {
      return getFileTypeImage(fc, null, name, size);
   }
   
   /**
    * Return the image path to the filetype icon for the specified file name string
    * 
    * @param sc         ServletContext
    * @param name       File name to build filetype icon path for
    * @param small      True for the small 16x16 icon or false for the large 32x32 
    * 
    * @return the image path for the specified node type or the default icon if not found
    */
   public static String getFileTypeImage(ServletContext sc, String name, boolean small)
   {
      return getFileTypeImage(null, sc, name, (small ? FileTypeImageSize.Small : FileTypeImageSize.Medium));
   }
   
   /**
    * Return the image path to the filetype icon for the specified file name string
    * 
    * @param sc         ServletContext
    * @param name       File name to build filetype icon path for
    * @param size       Size of the icon to return
    * 
    * @return the image path for the specified node type or the default icon if not found
    */
   public static String getFileTypeImage(ServletContext sc, String name, FileTypeImageSize size)
   {
      return getFileTypeImage(null, sc, name, size);
   }
   
   private static String getFileTypeImage(FacesContext fc, ServletContext sc, String name, FileTypeImageSize size)
   {
      String image = null;
      String defaultImage = null;
      switch (size)
      {
         case Small:
            defaultImage = DEFAULT_FILE_IMAGE16; break;
         case Medium:
            defaultImage = DEFAULT_FILE_IMAGE32; break;
         case Large:
            defaultImage = DEFAULT_FILE_IMAGE64; break;
      }
      
      int extIndex = name.lastIndexOf('.');
      if (extIndex != -1 && name.length() > extIndex + 1)
      {
         String ext = name.substring(extIndex + 1).toLowerCase();
         String key = ext + ' ' + size.toString();
         
         // found file extension for appropriate size image
         synchronized (s_fileExtensionMap)
         {
            image = s_fileExtensionMap.get(key);
            if (image == null)
            {
               // not found create for first time
               if (size != FileTypeImageSize.Large)
               {
                  image = (size == FileTypeImageSize.Small ? IMAGE_PREFIX16 : IMAGE_PREFIX32) +
                           ext + IMAGE_POSTFIX_GIF;
               }
               else
               {
                  image = IMAGE_PREFIX64 + ext + IMAGE_POSTFIX_PNG;
               }
               
               // does this image exist on the web-server?
               if ((fc != null && fc.getExternalContext().getResourceAsStream(image) != null) ||
                   (sc != null && sc.getResourceAsStream(image) != null))
               {
                  // found the image for this extension - save it for later
                  s_fileExtensionMap.put(key, image);
               }
               else if ((fc == null) && (sc == null))
               {
                  // we have neither FacesContext nor ServerContext so return the default image but don't cache it
                  image = defaultImage;
               }
               else
               {
                  // not found, save the default image for this extension instead
                  s_fileExtensionMap.put(key, defaultImage);
                  image = defaultImage;
               }
            }
         }
      }
      
      return (image != null ? image : defaultImage);
   }
   
   /**
    * Given a ConfigElement instance retrieve the display label, this could be
    * dervied from a message bundle key or a literal string
    * 
    * @param context FacesContext
    * @param configElement The ConfigElement to test
    * @return The resolved display label
    */
   public static String getDisplayLabel(FacesContext context, ConfigElement configElement)
   {
      String label = null;
 
      // look for a localized string
      String msgId = configElement.getAttribute("display-label-id");
      if (msgId != null)
      {
         label = Application.getMessage(context, msgId);
      }
      
      // if there wasn't an externalized string look for a literal string
      if (label == null)
      {
         label = configElement.getAttribute("display-label");
      }
      
      return label;
   }
   
   /**
    * Given a ConfigElement instance retrieve the description, this could be
    * dervied from a message bundle key or a literal string
    * 
    * @param context FacesContext
    * @param configElement The ConfigElement to test
    * @return The resolved description
    */
   public static String getDescription(FacesContext context, ConfigElement configElement)
   {
      String description = null;
      
      // look for a localized string
      String msgId = configElement.getAttribute("description-id");
      if (msgId != null)
      {
         description = Application.getMessage(context, msgId);
      }
      
      // if there wasn't an externalized string look for a literal string
      if (description == null)
      {
         description = configElement.getAttribute("description");
      }
      
      return description;
   }
}
