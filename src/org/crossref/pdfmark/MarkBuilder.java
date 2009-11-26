package org.crossref.pdfmark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.crossref.pdfmark.prism.Prism21Schema;
import org.crossref.pdfmark.unixref.JournalArticle;
import org.crossref.pdfmark.unixref.Unixref;

import com.lowagie.text.xml.xmp.DublinCoreSchema;
import com.lowagie.text.xml.xmp.XmpArray;
import com.lowagie.text.xml.xmp.XmpSchema;
import com.lowagie.text.xml.xmp.XmpWriter;

public abstract class MarkBuilder implements MetadataGrabber.Handler {

	private byte[] xmpData;
	
	@Override
	public void onMetadata(Unixref md) {
		try {
			if (md.getType() != Unixref.Type.JOURNAL) {
				onFailure(md.getDoi(), MetadataGrabber.CRUMMY_XML_CODE,
						"No journal article metadata for DOI.");
				return;
			}
		} catch (XPathExpressionException e) {
			onFailure(md.getDoi(), MetadataGrabber.CRUMMY_XML_CODE,
					"Could not determine if DOI has any journal article metadata.");
			return;
		}
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		try {
			JournalArticle article = md.getJournal().getArticle();
			
			XmpWriter writer = new XmpWriter(bout);
			
			XmpSchema dc = new DublinCoreSchema();
			addToSchema(dc, DublinCoreSchema.CREATOR, article.getContributors());
			addToSchema(dc, DublinCoreSchema.TITLE, article.getTitles());
			dc.setProperty(DublinCoreSchema.DATE, article.getDate());
			dc.setProperty(DublinCoreSchema.IDENTIFIER, md.getDoi());
			writer.addRdfDescription(dc);
			
			XmpSchema prism = new Prism21Schema();
			prism.setProperty(Prism21Schema.PUBLICATION_DATE, article.getDate());
			prism.setProperty(Prism21Schema.DOI, md.getDoi());
			writer.addRdfDescription(prism);
			
			writer.close();
			xmpData = bout.toByteArray();
			
		} catch (IOException e) {
			onFailure(md.getDoi(), MetadataGrabber.CLIENT_EXCEPTION_CODE,
					  e.toString());
		} catch (XPathExpressionException e) {
			onFailure(md.getDoi(), MetadataGrabber.CLIENT_EXCEPTION_CODE,
					  e.toString());
		}
	}
	
	/**
	 * Adds a list of values as a bag if the list size is greater than 1,
	 * or as a single element if the list size is 1.
	 */
	private static void addToSchema(XmpSchema schema, String key, String[] vals) {
		if (vals.length == 1) {
			schema.setProperty(key, vals[0]);
		} else if (vals.length > 1) {
			XmpArray bag = new XmpArray(XmpArray.UNORDERED);
			for (String val : vals) {
				bag.add(val);
			}
			schema.setProperty(key, bag);
		}
	}
	
	public byte[] getXmpData() {
		return xmpData;
	}

}