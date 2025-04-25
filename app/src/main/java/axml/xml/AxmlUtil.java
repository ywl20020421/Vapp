package axml.xml;

import org.xmlpull.v1.*;
import java.io.*;
import java.util.*;
import android.util.*;

public class AxmlUtil {
	public static class Config {
        public static StringPoolChunk.Encoding encoding= StringPoolChunk.Encoding.UNICODE;
        public static int defaultReferenceRadix=16;
    }

    public static void encodeFile(String input, String output) throws XmlPullParserException, IOException {
		XmlPullParserFactory f = XmlPullParserFactory.newInstance();
		f.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
		XmlPullParser p = f.newPullParser();
		p.setInput(new FileInputStream(input), "UTF-8");
		new FileOutputStream(output).write(encode(p));
    }
	
	public static byte[] encodeFile(String input) throws XmlPullParserException, IOException {
		XmlPullParserFactory f = XmlPullParserFactory.newInstance();
		f.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
		XmlPullParser p = f.newPullParser();
		p.setInput(new FileInputStream(input), "UTF-8");
		return encode(p);
    }

    public static byte[] encodeString(String xml) throws XmlPullParserException, IOException {
		XmlPullParserFactory f=XmlPullParserFactory.newInstance();
		f.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
		XmlPullParser p = f.newPullParser();
		p.setInput(new StringReader(xml));
		return encode(p);
    }

    public static byte[] encode(XmlPullParser p) throws XmlPullParserException, IOException {
		XmlChunk chunk = new XmlChunk();
		TagChunk current = null;
		for (int i=p.getEventType();i != XmlPullParser.END_DOCUMENT;i = p.next()) {
			switch (i) {
				case XmlPullParser.START_DOCUMENT:
					break;
				case XmlPullParser.START_TAG:
					current = new TagChunk(current == null ?chunk: current, p);
					break;
				case XmlPullParser.END_TAG:
					Chunk c=current.getParent();
					current = c instanceof TagChunk ?(TagChunk)c: null;
					break;
				case XmlPullParser.TEXT:
					break;
				default:
					break;

			}
		}
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		IntWriter w=new IntWriter(os);
		chunk.write(w);
		w.close();
		return os.toByteArray();
    }

	public static PrintStream out;

	public static StringBuilder decoded;

	public static void decode(String inf, String output) throws XmlPullParserException, IOException {
		out = new PrintStream(new File(output));
		AXmlResourceParser parser=new AXmlResourceParser();
		parser.open(new FileInputStream(inf));
		StringBuilder indent = new StringBuilder(10);
		final String indentStep = "   ";
		while (true) {
			int type = parser.next();
			if (type == XmlPullParser.END_DOCUMENT) {
				break;
			}
			switch (type) {
				case XmlPullParser.START_DOCUMENT:
					{
						log("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
						log("<!-- Decompiled with AxmlXml 1.0 -->");
						break;
					}
				case XmlPullParser.START_TAG:
					{
						if (parser.getAttributeCount() <= 0) {
							logs("%s<%s%s", indent,
								 getNamespacePrefix(parser.getPrefix()), parser.getName());

							indent.append(indentStep);

							int namespaceCountBefore=parser.getNamespaceCount(parser.getDepth() - 1);
							int namespaceCount=parser.getNamespaceCount(parser.getDepth());

							for (int i=namespaceCountBefore;i != namespaceCount;++i) {
								log("%sxmlns:%s=\"%s\"",
									indent,
									parser.getNamespacePrefix(i),
									parser.getNamespaceUri(i));
							}
							log(">");
						} else {
							log("%s<%s%s", indent,
								getNamespacePrefix(parser.getPrefix()), parser.getName());
							indent.append(indentStep);

							int namespaceCountBefore=parser.getNamespaceCount(parser.getDepth() - 1);
							int namespaceCount=parser.getNamespaceCount(parser.getDepth());

							for (int i=namespaceCountBefore;i != namespaceCount;++i) {
								log("%sxmlns:%s=\"%s\"",
									indent,
									parser.getNamespacePrefix(i),
									parser.getNamespaceUri(i));
							}

							for (int i=0;i != parser.getAttributeCount();++i) {
								if (i == (parser.getAttributeCount() - 1)) {
									logs("%s%s%s=\"%s\"", indent,
										 getNamespacePrefix(parser.getAttributePrefix(i)),
										 parser.getAttributeName(i),
										 getAttributeValue(parser, i));
								} else {
									log("%s%s%s=\"%s\"", indent,
										getNamespacePrefix(parser.getAttributePrefix(i)),
										parser.getAttributeName(i),
										getAttributeValue(parser, i));
								}
							}
							log(" >");
						}
						break;
					}
				case XmlPullParser.END_TAG:
					{
						indent.setLength(indent.length() - indentStep.length());
						log("%s</%s%s>", indent,
							getNamespacePrefix(parser.getPrefix()),
							parser.getName());
						break;
					}
				case XmlPullParser.TEXT:
					{
						log("%s%s", indent, parser.getText());
						break;
					}
			}
		}
		out.close();
	}

    public static String decode(InputStream inf) throws XmlPullParserException, IOException {
		decoded = new StringBuilder();
		AXmlResourceParser parser=new AXmlResourceParser();
		parser.open(inf);
		StringBuilder indent = new StringBuilder(10);
		final String indentStep = "   ";
		while (true) {
			int type=parser.next();
			if (type == XmlPullParser.END_DOCUMENT) {
				break;
			}
			switch (type) {
				case XmlPullParser.START_DOCUMENT:
					{
						log1("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
						log1("<!-- Decompiled with BotX Patcher -->");
						break;
					}
				case XmlPullParser.START_TAG:
					{
						if (parser.getAttributeCount() <= 0) {
							logs1("%s<%s%s", indent,
								  getNamespacePrefix(parser.getPrefix()), parser.getName());

							indent.append(indentStep);

							int namespaceCountBefore=parser.getNamespaceCount(parser.getDepth() - 1);
							int namespaceCount=parser.getNamespaceCount(parser.getDepth());

							for (int i=namespaceCountBefore;i != namespaceCount;++i) {
								log1("%sxmlns:%s=\"%s\"",
									 indent,
									 parser.getNamespacePrefix(i),
									 parser.getNamespaceUri(i));
							}
							log1(">");
						} else {
							log1("%s<%s%s", indent,
								 getNamespacePrefix(parser.getPrefix()), parser.getName());
							indent.append(indentStep);

							int namespaceCountBefore=parser.getNamespaceCount(parser.getDepth() - 1);
							int namespaceCount=parser.getNamespaceCount(parser.getDepth());

							for (int i=namespaceCountBefore;i != namespaceCount;++i) {
								log1("%sxmlns:%s=\"%s\"",
									 indent,
									 parser.getNamespacePrefix(i),
									 parser.getNamespaceUri(i));
							}

							for (int i=0;i != parser.getAttributeCount();++i) {
								if (i == (parser.getAttributeCount() - 1)) {
									logs1("%s%s%s=\"%s\"", indent,
										  getNamespacePrefix(parser.getAttributePrefix(i)),
										  parser.getAttributeName(i),
										  getAttributeValue(parser, i));
								} else {
									log1("%s%s%s=\"%s\"", indent,
										 getNamespacePrefix(parser.getAttributePrefix(i)),
										 parser.getAttributeName(i),
										 getAttributeValue(parser, i));
								}
							}
							log1(" >");
						}
						break;
					}
				case XmlPullParser.END_TAG:
					{
						indent.setLength(indent.length() - indentStep.length());
						log1("%s</%s%s>", indent,
							 getNamespacePrefix(parser.getPrefix()),
							 parser.getName());
						break;
					}
				case XmlPullParser.TEXT:
					{
						log1("%s%s", indent, parser.getText());
						break;
					}
			}
		}
		return decoded.toString();
	}

	private static String getNamespacePrefix(String prefix) {
		if (prefix == null || prefix.length() == 0) {
			return "";
		}
		return prefix + ":";
	}

	private static String getAttributeValue(AXmlResourceParser parser, int index) {
		int type=parser.getAttributeValueType(index);
		int data=parser.getAttributeValueData(index);
		if (type == TypedValue.TYPE_STRING) {
			return parser.getAttributeValue(index);
		}
		if (type == TypedValue.TYPE_ATTRIBUTE) {
			return String.format("?%s%08X", getPackage(data), data);
		}
		if (type == TypedValue.TYPE_REFERENCE) {
			return String.format("@%s%08X", getPackage(data), data);
		}
		if (type == TypedValue.TYPE_FLOAT) {
			return String.valueOf(Float.intBitsToFloat(data));
		}
		if (type == TypedValue.TYPE_INT_HEX) {
			return String.format("0x%08X", data);
		}
		if (type == TypedValue.TYPE_INT_BOOLEAN) {
			return data != 0 ?"true": "false";
		}
		if (type == TypedValue.TYPE_DIMENSION) {
			return Float.toString(complexToFloat(data)) +
				DIMENSION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
		}
		if (type == TypedValue.TYPE_FRACTION) {
			return Float.toString(complexToFloat(data)) +
				FRACTION_UNITS[data & TypedValue.COMPLEX_UNIT_MASK];
		}
		if (type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT) {
			return String.format("#%08X", data);
		}
		if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) {
			return String.valueOf(data);
		}
		return String.format("<0x%X, type 0x%02X>", data, type);
	}

	private static String getPackage(int id) {
		if (id >>> 24 == 1) {
			return "android:";
		}
		return "";
	}

	private static void log(String format, Object...arguments) {
		out.printf(format, arguments);
		out.println();
	}

    private static void log1(String format, Object...arguments) {
		decoded.append(String.format(format, arguments));
		decoded.append("\n");
    }

	private static void logs(String format, Object...arguments) {
		out.printf(format, arguments);
	}

    private static void logs1(String format, Object...arguments) {
		decoded.append(String.format(format, arguments));
    }

	/////////////////////////////////// ILLEGAL STUFF, DONT LOOK :)

	public static float complexToFloat(int complex) {
		return (float)(complex & 0xFFFFFF00) * RADIX_MULTS[(complex >> 4) & 3];
	}

	private static final float RADIX_MULTS[]={
		0.00390625F,3.051758E-005F,1.192093E-007F,4.656613E-010F
	};
	private static final String DIMENSION_UNITS[]={
		"px","dip","sp","pt","in","mm","",""
	};
	private static final String FRACTION_UNITS[]={
		"%","%p","","","","","",""
	};
}

