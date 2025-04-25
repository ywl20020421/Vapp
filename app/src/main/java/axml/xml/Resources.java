package axml.xml;

import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.util.regex.*;
import org.xmlpull.v1.*;
import android.util.*;

public class Resources {

    public HashMap<String,Integer> attrMap=new HashMap<>();

    private void init() {
        try {
            Field[] fs = android.R.attr.class.getFields();
            for (Field f : fs) attrMap.put(f.getName(), (Integer)f.get(null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Resources() {
        init();
    }

    public int getIdentifier(String name, String type, String pkg) {
        if ("android".equals(pkg) && "attr".equals(type)) {
            Integer x=attrMap.get(name);
            if (x == null) System.out.println("attr not found: " + name);
            else {
                System.out.format("@%s:%s/%s=0x%x\n", pkg, type, name, x);
                return x;
            }
        } else {
            System.out.format("@%s:%s/%s=0x%x\n", pkg, type, name, 0);
        }
        return 0;
    }
}

class ChunkUtil {

	public static final void readCheckType(IntReader reader, int expectedType) throws IOException {
		int type=reader.readInt();
		if (type != expectedType) {
			throw new IOException(
				"Expected chunk of type 0x" + Integer.toHexString(expectedType) +
				", read 0x" + Integer.toHexString(type) + ".");
		}
	}
}

class ComplexConsts {
    // Where the unit type information is.  This gives us 16 possible
    // types, as defined below.
    public static final int UNIT_SHIFT = 0;
    public static final int UNIT_MASK = 0xf;

    // TYPE_DIMENSION: Value is raw pixels.
    public static final int UNIT_PX = 0;
    // TYPE_DIMENSION: Value is Device Independent Pixels.
    public static final int UNIT_DIP = 1;
    // TYPE_DIMENSION: Value is a Scaled device independent Pixels.
    public static final int UNIT_SP = 2;
    // TYPE_DIMENSION: Value is in points.
    public static final int UNIT_PT = 3;
    // TYPE_DIMENSION: Value is in inches.
    public static final int UNIT_IN = 4;
    // TYPE_DIMENSION: Value is in millimeters.
    public static final int UNIT_MM = 5;

    // TYPE_FRACTION: A basic fraction of the overall size.
    public static final int UNIT_FRACTION = 0;
    // TYPE_FRACTION: A fraction of the parent size.
    public static final int UNIT_FRACTION_PARENT = 1;

    // Where the radix information is, telling where the decimal place
    // appears in the mantissa.  This give us 4 possible fixed point
    // representations as defined below.
    public static final int RADIX_SHIFT = 4;
    public static final int RADIX_MASK = 0x3;

    // The mantissa is an integral number -- i.e., 0xnnnnnn.0
    public static final int RADIX_23p0 = 0;
    // The mantissa magnitude is 16 bits -- i.e, 0xnnnn.nn
    public static final int RADIX_16p7 = 1;
    // The mantissa magnitude is 8 bits -- i.e, 0xnn.nnnn
    public static final int RADIX_8p15 = 2;
    // The mantissa magnitude is 0 bits -- i.e, 0x0.nnnnnn
    public static final int RADIX_0p23 = 3;

    // Where the actual value is.  This gives us 23 bits of
    // precision.  The top bit is the sign.
    public static final int MANTISSA_SHIFT = 8;
    public static final int MANTISSA_MASK = 0xffffff;
}

class DefaultReferenceResolver implements ReferenceResolver {
    static Pattern pat = Pattern.compile("^@\\+?(?:(\\w+):)?(?:(\\w+)/)?(\\w+)$");

    public int resolve(ValueChunk value, String ref) {
		Matcher m=pat.matcher(ref);
		if (!m.matches()) throw new RuntimeException("invalid reference");
		String pkg=m.group(1);
		String type=m.group(2);
		String name=m.group(3);
		try {
			return Integer.parseInt(name, AxmlUtil.Config.defaultReferenceRadix);
		} catch (Exception e) {
			e.printStackTrace();
		}
		int id=value.getContext().getResources().getIdentifier(name, type, pkg);
		return id;

    }
}

class IntReader {

	public IntReader() {
	}
	public IntReader(InputStream stream, boolean bigEndian) {
		reset(stream, bigEndian);
	}

	public final void reset(InputStream stream, boolean bigEndian) {
		m_stream = stream;
		m_bigEndian = bigEndian;
		m_position = 0;		
	}

	public final void close() {
		if (m_stream == null) {
			return;
		}
		try {
			m_stream.close();
		} catch (IOException e) {
		}
		reset(null, false);
	}

	public final InputStream getStream() {
		return m_stream;
	}

	public final boolean isBigEndian() {
		return m_bigEndian;
	}
	public final void setBigEndian(boolean bigEndian) {
		m_bigEndian = bigEndian;
	}

	public final int readByte() throws IOException {
		return readInt(1);
	}
	public final int readShort() throws IOException {
		return readInt(2);
	}
	public final int readInt() throws IOException {
		return readInt(4);
	}

	public final int readInt(int length) throws IOException {
		if (length < 0 || length > 4) {
			throw new IllegalArgumentException();
		}
		int result=0;
		if (m_bigEndian) {
			for (int i=(length - 1) * 8;i >= 0;i -= 8) {
				int b=m_stream.read();
				if (b == -1) {
					throw new EOFException();
				}
				m_position += 1;
				result |= (b << i);
			}
		} else {
			length *= 8;
			for (int i=0;i != length;i += 8) {
				int b=m_stream.read();
				if (b == -1) {
					throw new EOFException();
				}
				m_position += 1;
				result |= (b << i);
			}
		}
		return result;		  
	}

	public final int[] readIntArray(int length) throws IOException {
		int[] array=new int[length];
		readIntArray(array, 0, length);
		return array;
	}

	public final void readIntArray(int[] array, int offset, int length) throws IOException {
		for (;length > 0;length -= 1) {
			array[offset++] = readInt();
		}
	}

	public final byte[] readByteArray(int length) throws IOException {
		byte[] array=new byte[length];
		int read=m_stream.read(array);
		m_position += read;
		if (read != length) {
			throw new EOFException();
		}
		return array;
	}

	public final void skip(int bytes) throws IOException {
		if (bytes <= 0) {
			return;
		}
		long skipped=m_stream.skip(bytes);
		m_position += skipped;
		if (skipped != bytes) {
			throw new EOFException();
		}
	}

	public final void skipInt() throws IOException {
		skip(4);
	}

	public final int available() throws IOException {
		return m_stream.available();
	}

	public final int getPosition() {
		return m_position;
	}

	/////////////////////////////////// data

	private InputStream m_stream;
	private boolean m_bigEndian;
	private int m_position;
}

interface ReferenceResolver {
    int resolve(ValueChunk value, String ref);
}

class StringBlock {

	public static StringBlock read(IntReader reader) throws IOException {
		ChunkUtil.readCheckType(reader, 0x001C0001);
		int chunkSize=reader.readInt();
		int stringCount=reader.readInt();
		int styleOffsetCount=reader.readInt();
		/*?*/reader.readInt();
		int stringsOffset=reader.readInt();
		int stylesOffset=reader.readInt();

		StringBlock block=new StringBlock();
		block.m_stringOffsets = reader.readIntArray(stringCount);
		if (styleOffsetCount != 0) {
			block.m_styleOffsets = reader.readIntArray(styleOffsetCount);
		}
		{
			int size=((stylesOffset == 0) ?chunkSize: stylesOffset) - stringsOffset;
			if ((size % 4) != 0) {
				throw new IOException("String data size is not multiple of 4 (" + size + ").");
			}
			block.m_strings = reader.readIntArray(size / 4);
		}
		if (stylesOffset != 0) {
			int size=(chunkSize - stylesOffset);
			if ((size % 4) != 0) {
				throw new IOException("Style data size is not multiple of 4 (" + size + ").");
			}
			block.m_styles = reader.readIntArray(size / 4);
		}

		return block;	
	}

	/**
	 * Returns number of strings in block. 
	 */
	public int getCount() {
		return m_stringOffsets != null ?
			m_stringOffsets.length:
			0;
	}

	/**
	 * Returns raw string (without any styling information) at specified index.
	 */
	public String getString(int index) {
		if (index < 0 ||
			m_stringOffsets == null ||
			index >= m_stringOffsets.length) {
			return null;
		}
		int offset=m_stringOffsets[index];
		int length=getShort(m_strings, offset);
		StringBuilder result=new StringBuilder(length);
		for (;length != 0;length -= 1) {
			offset += 2;
			result.append((char)getShort(m_strings, offset));
		}
		return result.toString();
	}

	/**
	 * Not yet implemented. 
	 * 
	 * Returns string with style information (if any).
	 */
	public CharSequence get(int index) {
		return getString(index);
	}

	/**
	 * Returns string with style tags (html-like). 
	 */
	public String getHTML(int index) {
		String raw=getString(index);
		if (raw == null) {
			return raw;
		}
		int[] style=getStyle(index);
		if (style == null) {
			return raw;
		}
		StringBuilder html=new StringBuilder(raw.length() + 32);
		int offset=0;
		while (true) {
			int i=-1;
			for (int j=0;j != style.length;j += 3) {
				if (style[j + 1] == -1) {
					continue;
				}
				if (i == -1 || style[i + 1] > style[j + 1]) {
					i = j;
				}
			}
			int start=((i != -1) ?style[i + 1]: raw.length());
			for (int j=0;j != style.length;j += 3) {
				int end=style[j + 2];
				if (end == -1 || end >= start) {
					continue;
				}
				if (offset <= end) {
					html.append(raw, offset, end + 1);
					offset = end + 1;
				}
				style[j + 2] = -1;
				html.append('<');
				html.append('/');
				html.append(getString(style[j]));
				html.append('>');
			}
			if (offset < start) {
				html.append(raw, offset, start);
				offset = start;
			}
			if (i == -1) {
				break;
			}
			html.append('<');
			html.append(getString(style[i]));
			html.append('>');
			style[i + 1] = -1;
		}
		return html.toString();
	}

	/**
	 * Finds index of the string.
	 * Returns -1 if the string was not found.
	 */
	public int find(String string) {
		if (string == null) {
			return -1;
		}
		for (int i=0;i != m_stringOffsets.length;++i) {
			int offset=m_stringOffsets[i];
			int length=getShort(m_strings, offset);
			if (length != string.length()) {
				continue;
			}
			int j=0;
			for (;j != length;++j) {
				offset += 2;
				if (string.charAt(j) != getShort(m_strings, offset)) {
					break;
				}
			}
			if (j == length) {
				return i;
			}
		}
		return -1;
	}

	///////////////////////////////////////////// implementation

	private StringBlock() {
	}

	/**
	 * Returns style information - array of int triplets,
	 * where in each triplet:
	 * 	* first int is index of tag name ('b','i', etc.)
	 * 	* second int is tag start index in string
	 * 	* third int is tag end index in string
	 */
	private int[] getStyle(int index) {
		if (m_styleOffsets == null || m_styles == null ||
			index >= m_styleOffsets.length) {
			return null;
		}
		int offset=m_styleOffsets[index] / 4;
		int style[];
		{
			int count=0;
			for (int i=offset;i < m_styles.length;++i) {
				if (m_styles[i] == -1) {
					break;
				}
				count += 1;
			}
			if (count == 0 || (count % 3) != 0) {
				return null;
			}
			style = new int[count];
		}
		for (int i=offset,j=0;i < m_styles.length;) {
			if (m_styles[i] == -1) {
				break;
			}
			style[j++] = m_styles[i++];
		}
		return style;
	}

	private static final int getShort(int[] array, int offset) {
		int value=array[offset / 4];
		if ((offset % 4) / 2 == 0) {
			return (value & 0xFFFF);
		} else {
			return (value >>> 16);
		}
	}

	private int[] m_stringOffsets;
	private int[] m_strings;
	private int[] m_styleOffsets;
	private int[] m_styles;

}

class ValueType {
    // Contains no data.
    public static final  byte NULL = 0x00;
    // The 'data' holds a ResTable_ref, a reference to another resource
    // table entry.
    public static final  byte REFERENCE = 0x01;
    // The 'data' holds an attribute resource identifier.
    public static final  byte ATTRIBUTE = 0x02;
    // The 'data' holds an index into the containing resource table's
    // global value string pool.
    public static final  byte STRING = 0x03;
    // The 'data' holds a single-precision floating point number.
    public static final  byte FLOAT = 0x04;
    // The 'data' holds a complex number encoding a dimension value;
    // such as "100in".
    public static final  byte DIMENSION = 0x05;
    // The 'data' holds a complex number encoding a fraction of a
    // container.
    public static final  byte FRACTION = 0x06;

    // Beginning of integer flavors...
    public static final  byte FIRST_INT = 0x10;

    // The 'data' is a raw integer value of the form n..n.
    public static final  byte INT_DEC = 0x10;
    // The 'data' is a raw integer value of the form 0xn..n.
    public static final  byte INT_HEX = 0x11;
    // The 'data' is either 0 or 1, for input "false" or "true" respectively.
    public static final  byte INT_BOOLEAN = 0x12;

    // Beginning of color integer flavors...
    public static final  byte FIRST_COLOR_INT = 0x1c;

    // The 'data' is a raw integer value of the form #aarrggbb.
    public static final  byte INT_COLOR_ARGB8 = 0x1c;
    // The 'data' is a raw integer value of the form #rrggbb.
    public static final  byte INT_COLOR_RGB8 = 0x1d;
    // The 'data' is a raw integer value of the form #argb.
    public static final  byte INT_COLOR_ARGB4 = 0x1e;
    // The 'data' is a raw integer value of the form #rgb.
    public static final  byte INT_COLOR_RGB4 = 0x1f;

    // ...end of integer flavors.
    public static final  byte LAST_COLOR_INT = 0x1f;

    // ...end of integer flavors.
    public static final  byte LAST_INT = 0x1f;
}

interface XmlResourceParser extends XmlPullParser, AttributeSet {
	void close();

	@Override
	int getAttributeNameResource(int i);

	@Override
	int getAttributeListValue(String s, String s1, String[] strings, int i);

	@Override
	boolean getAttributeBooleanValue(String s, String s1, boolean b);

	@Override
	int getAttributeResourceValue(String s, String s1, int i);

	@Override
	int getAttributeIntValue(String s, String s1, int i);

	@Override
	int getAttributeUnsignedIntValue(String s, String s1, int i);

	@Override
	float getAttributeFloatValue(String s, String s1, float v);

	@Override
	int getAttributeListValue(int i, String[] strings, int i1);

	@Override
	boolean getAttributeBooleanValue(int i, boolean b);

	@Override
	int getAttributeResourceValue(int i, int i1);

	@Override
	int getAttributeIntValue(int i, int i1);

	@Override
	int getAttributeUnsignedIntValue(int i, int i1);

	@Override
	float getAttributeFloatValue(int i, float v);

	@Override
	String getIdAttribute();

	@Override
	String getClassAttribute();

	@Override
	int getIdAttributeResourceValue(int i);

	@Override
	int getStyleAttribute();

	@Override
	void setFeature(String s, boolean b) throws XmlPullParserException;

	@Override
	boolean getFeature(String s);

	@Override
	void setProperty(String s, Object o) throws XmlPullParserException;

	@Override
	Object getProperty(String s);

	@Override
	void setInput(Reader reader) throws XmlPullParserException;

	@Override
	void setInput(InputStream inputStream, String s) throws XmlPullParserException;

	@Override
	String getInputEncoding();

	@Override
	void defineEntityReplacementText(String s, String s1) throws XmlPullParserException;

	@Override
	int getNamespaceCount(int i) throws XmlPullParserException;

	@Override
	String getNamespacePrefix(int i) throws XmlPullParserException;

	@Override
	String getNamespaceUri(int i) throws XmlPullParserException;

	@Override
	String getNamespace(String s);

	@Override
	int getDepth();

	@Override
	String getPositionDescription();

	@Override
	int getLineNumber();

	@Override
	int getColumnNumber();

	@Override
	boolean isWhitespace() throws XmlPullParserException;

	@Override
	String getText();

	@Override
	char[] getTextCharacters(int[] ints);

	@Override
	String getNamespace();

	@Override
	String getName();

	@Override
	String getPrefix();

	@Override
	boolean isEmptyElementTag() throws XmlPullParserException;

	@Override
	int getAttributeCount();

	@Override
	String getAttributeNamespace(int i);

	@Override
	String getAttributeName(int i);

	@Override
	String getAttributePrefix(int i);

	@Override
	String getAttributeType(int i);

	@Override
	boolean isAttributeDefault(int i);

	@Override
	String getAttributeValue(int i);

	@Override
	String getAttributeValue(String s, String s1);

	@Override
	int getEventType() throws XmlPullParserException;

	@Override
	int next() throws IOException, XmlPullParserException;

	@Override
	int nextToken() throws IOException, XmlPullParserException;

	@Override
	void require(int i, String s, String s1) throws IOException, XmlPullParserException;

	@Override
	String nextText() throws IOException, XmlPullParserException;

	@Override
	int nextTag() throws IOException, XmlPullParserException;
}

class AXmlResourceParser implements XmlResourceParser {

	public AXmlResourceParser() {
		resetEventInfo();
	}

	public void open(InputStream stream) {
		close();
		if (stream != null) {
			m_reader = new IntReader(stream, false);
		}
	}

	public void close() {
		if (!m_operational) {
			return;
		}
		m_operational = false;
		m_reader.close();
		m_reader = null;
		m_strings = null;
		m_resourceIDs = null;
		m_namespaces.reset();
		resetEventInfo();
	}

	/////////////////////////////////// iteration

	public int next() throws XmlPullParserException,IOException {
		if (m_reader == null) {
			throw new XmlPullParserException("Parser is not opened.", this, null);
		}
		try {
			doNext();
			return m_event;			
		} catch (IOException e) {
			close();
			throw e;
		}
	}

	public int nextToken() throws XmlPullParserException,IOException {
		return next();
	}

	public int nextTag() throws XmlPullParserException,IOException {
		int eventType=next();
		if (eventType == TEXT && isWhitespace()) {
			eventType = next();
		}
		if (eventType != START_TAG && eventType != END_TAG) {
			throw new XmlPullParserException("Expected start or end tag.", this, null);
		}
		return eventType;
	}

	public String nextText() throws XmlPullParserException,IOException {
		if (getEventType() != START_TAG) {
			throw new XmlPullParserException("Parser must be on START_TAG to read next text.", this, null);
		}
		int eventType=next();
		if (eventType == TEXT) {
			String result=getText();
			eventType = next();
			if (eventType != END_TAG) {
				throw new XmlPullParserException("Event TEXT must be immediately followed by END_TAG.", this, null);
			}
			return result;
		} else if (eventType == END_TAG) {
			return "";
		} else {
			throw new XmlPullParserException("Parser must be on START_TAG or TEXT to read text.", this, null);
		}
	}

	public void require(int type, String namespace, String name) throws XmlPullParserException,IOException {
		if (type != getEventType() ||
			(namespace != null && !namespace.equals(getNamespace())) ||
			(name != null && !name.equals(getName()))) {
			throw new XmlPullParserException(TYPES[type] + " is expected.", this, null);
		}
	}

	public int getDepth() {
		return m_namespaces.getDepth() - 1;
	}

	public int getEventType() throws XmlPullParserException {
		return m_event;
	}

	public int getLineNumber() {
		return m_lineNumber;
	}

	public String getName() {
		if (m_name == -1 || (m_event != START_TAG && m_event != END_TAG)) {
			return null;
		}
		return m_strings.getString(m_name);
	}

	public String getText() {
		if (m_name == -1 || m_event != TEXT) {
			return null;
		}
		return m_strings.getString(m_name);
	}

	public char[] getTextCharacters(int[] holderForStartAndLength) {
		String text=getText();
		if (text == null) {
			return null;
		}
		holderForStartAndLength[0] = 0;
		holderForStartAndLength[1] = text.length();
		char[] chars=new char[text.length()];
		text.getChars(0, text.length(), chars, 0);
		return chars;
	}

	public String getNamespace() {
		return m_strings.getString(m_namespaceUri);
	}

	public String getPrefix() {
		int prefix=m_namespaces.findPrefix(m_namespaceUri);
		return m_strings.getString(prefix);
	}

	public String getPositionDescription() {
		return "XML line #" + getLineNumber();
	}

	public int getNamespaceCount(int depth) throws XmlPullParserException {
		return m_namespaces.getAccumulatedCount(depth);
	}

	public String getNamespacePrefix(int pos) throws XmlPullParserException {
		int prefix=m_namespaces.getPrefix(pos);
		return m_strings.getString(prefix);
	}

	public String getNamespaceUri(int pos) throws XmlPullParserException {
		int uri=m_namespaces.getUri(pos);
		return m_strings.getString(uri);
	}

	/////////////////////////////////// attributes

	public String getClassAttribute() {
		if (m_classAttribute == -1) {
			return null;
		}
		int offset=getAttributeOffset(m_classAttribute);
		int value=m_attributes[offset + ATTRIBUTE_IX_VALUE_STRING];
		return m_strings.getString(value);
	}

	public String getIdAttribute() {
		if (m_idAttribute == -1) {
			return null;
		}
		int offset=getAttributeOffset(m_idAttribute);
		int value=m_attributes[offset + ATTRIBUTE_IX_VALUE_STRING];
		return m_strings.getString(value);
	}

	public int getIdAttributeResourceValue(int defaultValue) {
		if (m_idAttribute == -1) {
			return defaultValue;
		}
		int offset=getAttributeOffset(m_idAttribute);
		int valueType=m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
		if (valueType != TypedValue.TYPE_REFERENCE) {
			return defaultValue;
		}
		return m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
	}

	public int getStyleAttribute() {
		if (m_styleAttribute == -1) {
			return 0;
		}
		int offset=getAttributeOffset(m_styleAttribute);
		return m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
	}

	public int getAttributeCount() {
		if (m_event != START_TAG) {
			return -1;
		}
		return m_attributes.length / ATTRIBUTE_LENGHT;
	}

	public String getAttributeNamespace(int index) {
		int offset=getAttributeOffset(index);
		int namespace=m_attributes[offset + ATTRIBUTE_IX_NAMESPACE_URI];
		if (namespace == -1) {
			return "";
		}
		return m_strings.getString(namespace);
	}

	public String getAttributePrefix(int index) {
		int offset=getAttributeOffset(index);
		int uri=m_attributes[offset + ATTRIBUTE_IX_NAMESPACE_URI];
		int prefix=m_namespaces.findPrefix(uri);
		if (prefix == -1) {
			return "";
		}
		return m_strings.getString(prefix);
	}

	public String getAttributeName(int index) {
		int offset=getAttributeOffset(index);
		int name=m_attributes[offset + ATTRIBUTE_IX_NAME];
		if (name == -1) {
			return "";
		}
		return m_strings.getString(name);
	}

	public int getAttributeNameResource(int index) {
		int offset=getAttributeOffset(index);
		int name=m_attributes[offset + ATTRIBUTE_IX_NAME];
		if (m_resourceIDs == null ||
			name < 0 || name >= m_resourceIDs.length) {
			return 0;
		}
		return m_resourceIDs[name];
	}

	public int getAttributeValueType(int index) {
		int offset=getAttributeOffset(index);
		return m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
	}

	public int getAttributeValueData(int index) {
		int offset=getAttributeOffset(index);
		return m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
	}

	public String getAttributeValue(int index) {
		int offset=getAttributeOffset(index);
		int valueType=m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
		if (valueType == TypedValue.TYPE_STRING) {
			int valueString=m_attributes[offset + ATTRIBUTE_IX_VALUE_STRING];
			return m_strings.getString(valueString);
		}
		int valueData=m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
		return "";//TypedValue.coerceToString(valueType,valueData);
	}

	public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
		return getAttributeIntValue(index, defaultValue ?1: 0) != 0;
	}

	public float getAttributeFloatValue(int index, float defaultValue) {
		int offset=getAttributeOffset(index);
		int valueType=m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
		if (valueType == TypedValue.TYPE_FLOAT) {
			int valueData=m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
			return Float.intBitsToFloat(valueData);
		}
		return defaultValue;
	}

	public int getAttributeIntValue(int index, int defaultValue) {
		int offset=getAttributeOffset(index);
		int valueType=m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
		if (valueType >= TypedValue.TYPE_FIRST_INT &&
			valueType <= TypedValue.TYPE_LAST_INT) {
			return m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
		}
		return defaultValue;
	}

	public int getAttributeUnsignedIntValue(int index, int defaultValue) {
		return getAttributeIntValue(index, defaultValue);
	}

	public int getAttributeResourceValue(int index, int defaultValue) {
		int offset=getAttributeOffset(index);
		int valueType=m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
		if (valueType == TypedValue.TYPE_REFERENCE) {
			return m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
		}
		return defaultValue;
	}

	public String getAttributeValue(String namespace, String attribute) {
		int index=findAttribute(namespace, attribute);
		if (index == -1) {
			return null;
		}
		return getAttributeValue(index);
	}

	public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
		int index=findAttribute(namespace, attribute);
		if (index == -1) {
			return defaultValue;
		}
		return getAttributeBooleanValue(index, defaultValue);
	}

	public float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
		int index=findAttribute(namespace, attribute);
		if (index == -1) {
			return defaultValue;
		}
		return getAttributeFloatValue(index, defaultValue);
	}

	public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
		int index=findAttribute(namespace, attribute);
		if (index == -1) {
			return defaultValue;
		}
		return getAttributeIntValue(index, defaultValue);
	}

	public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
		int index=findAttribute(namespace, attribute);
		if (index == -1) {
			return defaultValue;
		}
		return getAttributeUnsignedIntValue(index, defaultValue);
	}

	public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
		int index=findAttribute(namespace, attribute);
		if (index == -1) {
			return defaultValue;
		}
		return getAttributeResourceValue(index, defaultValue);
	}

	public int getAttributeListValue(int index, String[] options, int defaultValue) {
		// TODO implement
		return 0;
	}

	public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
		// TODO implement
		return 0;
	}

	public String getAttributeType(int index) {
		return "CDATA";
	}

	public boolean isAttributeDefault(int index) {
		return false;
	}

	/////////////////////////////////// dummies

	public void setInput(InputStream stream, String inputEncoding) throws XmlPullParserException {
		throw new XmlPullParserException(E_NOT_SUPPORTED);
	}
	public void setInput(Reader reader) throws XmlPullParserException {
		throw new XmlPullParserException(E_NOT_SUPPORTED);
	}

	public String getInputEncoding() {
		return null;
	}

	public int getColumnNumber() {
		return -1;
	}

	public boolean isEmptyElementTag() throws XmlPullParserException {
		return false;
	}

	public boolean isWhitespace() throws XmlPullParserException {
		return false;
	}

	public void defineEntityReplacementText(String entityName, String replacementText) throws XmlPullParserException {
		throw new XmlPullParserException(E_NOT_SUPPORTED);
	}

	public String getNamespace(String prefix) {
		throw new RuntimeException(E_NOT_SUPPORTED);
	}

	public Object getProperty(String name) {
		return null;
	}
	public void setProperty(String name, Object value) throws XmlPullParserException {
		throw new XmlPullParserException(E_NOT_SUPPORTED);
	}

	public boolean getFeature(String feature) {
		return false;
	}
	public void setFeature(String name, boolean value) throws XmlPullParserException {
		throw new XmlPullParserException(E_NOT_SUPPORTED);
	}

	///////////////////////////////////////////// implementation

	/**
	 * Namespace stack, holds prefix+uri pairs, as well as
	 *  depth information.
	 * All information is stored in one int[] array.
	 * Array consists of depth frames:
	 *  Data=DepthFrame*;
	 *  DepthFrame=Count+[Prefix+Uri]*+Count;
	 *  Count='count of Prefix+Uri pairs';
	 * Yes, count is stored twice, to enable bottom-up traversal.
	 * increaseDepth adds depth frame, decreaseDepth removes it.
	 * push/pop operations operate only in current depth frame.
	 * decreaseDepth removes any remaining (not pop'ed) namespace pairs.
	 * findXXX methods search all depth frames starting 
	 * from the last namespace pair of current depth frame.
	 * All functions that operate with int, use -1 as 'invalid value'.
	 * 
	 * !! functions expect 'prefix'+'uri' pairs, not 'uri'+'prefix' !!
	 * 
	 */
	private static final class NamespaceStack {
		public NamespaceStack() {
			m_data = new int[32];
		}

		public final void reset() {
			m_dataLength = 0;
			m_count = 0;
			m_depth = 0;
		}

		public final int getTotalCount() {
			return m_count;
		}

		public final int getCurrentCount() {
			if (m_dataLength == 0) {
				return 0;
			}
			int offset=m_dataLength - 1;
			return m_data[offset];
		}

		public final int getAccumulatedCount(int depth) {
			if (m_dataLength == 0 || depth < 0) {
				return 0;
			}
			if (depth > m_depth) {
				depth = m_depth;
			}
			int accumulatedCount=0;
			int offset=0;
			for (;depth != 0;--depth) {
				int count=m_data[offset];
				accumulatedCount += count;
				offset += (2 + count * 2);
			}
			return accumulatedCount;
		}

		public final void push(int prefix, int uri) {
			if (m_depth == 0) {
				increaseDepth();
			}
			ensureDataCapacity(2);
			int offset=m_dataLength - 1;
			int count=m_data[offset];
			m_data[offset - 1 - count * 2] = count + 1;
			m_data[offset] = prefix;
			m_data[offset + 1] = uri;
			m_data[offset + 2] = count + 1;
			m_dataLength += 2;
			m_count += 1;
		}

		public final boolean pop(int prefix, int uri) {
			if (m_dataLength == 0) {
				return false;
			}
			int offset=m_dataLength - 1;
			int count=m_data[offset];
			for (int i=0,o=offset - 2;i != count;++i,o -= 2) {
				if (m_data[o] != prefix || m_data[o + 1] != uri) {
					continue;
				}
				count -= 1;
				if (i == 0) {
					m_data[o] = count;
					o -= (1 + count * 2);
					m_data[o] = count;
				} else {
					m_data[offset] = count;
					offset -= (1 + 2 + count * 2);
					m_data[offset] = count;
					System.arraycopy(
						m_data, o + 2,
						m_data, o,
						m_dataLength - o);
				}
				m_dataLength -= 2;
				m_count -= 1;
				return true;
			}
			return false;
		}

		public final boolean pop() {
			if (m_dataLength == 0) {
				return false;
			}
			int offset=m_dataLength - 1;
			int count=m_data[offset];
			if (count == 0) {
				return false;
			}
			count -= 1;
			offset -= 2;
			m_data[offset] = count;
			offset -= (1 + count * 2);
			m_data[offset] = count;
			m_dataLength -= 2;
			m_count -= 1;
			return true;			
		}

		public final int getPrefix(int index) {
			return get(index, true);
		}

		public final int getUri(int index) {
			return get(index, false);
		}

		public final int findPrefix(int uri) {
			return find(uri, false);
		}

		public final int findUri(int prefix) {
			return find(prefix, true);
		}

		public final int getDepth() {
			return m_depth;
		}

		public final void increaseDepth() {
			ensureDataCapacity(2);
			int offset=m_dataLength;
			m_data[offset] = 0;
			m_data[offset + 1] = 0;
			m_dataLength += 2;
			m_depth += 1;
		}
		public final void decreaseDepth() {
			if (m_dataLength == 0) {
				return;
			}
			int offset=m_dataLength - 1;
			int count=m_data[offset];
			if ((offset - 1 - count * 2) == 0) {
				return;
			}
			m_dataLength -= 2 + count * 2;
			m_count -= count;
			m_depth -= 1;
		}

		private void ensureDataCapacity(int capacity) {
			int available=(m_data.length - m_dataLength);
			if (available > capacity) {
				return;
			}
			int newLength=(m_data.length + available) * 2;
			int[] newData=new int[newLength];
			System.arraycopy(m_data, 0, newData, 0, m_dataLength);
			m_data = newData;
		}

		private final int find(int prefixOrUri, boolean prefix) {
			if (m_dataLength == 0) {
				return -1;
			}			
			int offset=m_dataLength - 1;
			for (int i=m_depth;i != 0;--i) {
				int count=m_data[offset];
				offset -= 2;
				for (;count != 0;--count) {
					if (prefix) {
						if (m_data[offset] == prefixOrUri) {
							return m_data[offset + 1];
						}
					} else {
						if (m_data[offset + 1] == prefixOrUri) {
							return m_data[offset];
						}
					}
					offset -= 2;
				}
			}
			return -1;
		}

		private final int get(int index, boolean prefix) {
			if (m_dataLength == 0 || index < 0) {
				return -1;
			}
			int offset=0;
			for (int i=m_depth;i != 0;--i) {
				int count=m_data[offset];
				if (index >= count) {
					index -= count;
					offset += (2 + count * 2);
					continue;
				}
				offset += (1 + index * 2);
				if (!prefix) {
					offset += 1;
				}
				return m_data[offset];
			}
			return -1;
		}

		private int[] m_data;
		private int m_dataLength;
		private int m_count;
		private int m_depth;
	}

	/////////////////////////////////// package-visible

//	final void fetchAttributes(int[] styleableIDs,TypedArray result) {
//		result.resetIndices();
//		if (m_attributes==null || m_resourceIDs==null) {
//			return;
//		}
//		boolean needStrings=false;
//		for (int i=0,e=styleableIDs.length;i!=e;++i) {
//			int id=styleableIDs[i];
//			for (int o=0;o!=m_attributes.length;o+=ATTRIBUTE_LENGHT) {
//				int name=m_attributes[o+ATTRIBUTE_IX_NAME];
//				if (name>=m_resourceIDs.length ||
//					m_resourceIDs[name]!=id)
//				{
//					continue;
//				}
//				int valueType=m_attributes[o+ATTRIBUTE_IX_VALUE_TYPE];
//				int valueData;
//				int assetCookie;
//				if (valueType==TypedValue.TYPE_STRING) {
//					valueData=m_attributes[o+ATTRIBUTE_IX_VALUE_STRING];
//					assetCookie=-1;
//					needStrings=true;
//				} else {
//					valueData=m_attributes[o+ATTRIBUTE_IX_VALUE_DATA];
//					assetCookie=0;
//				}
//				result.addValue(i,valueType,valueData,assetCookie,id,0);
//			}
//		}
//		if (needStrings) {
//			result.setStrings(m_strings);
//		}
//	}

	final StringBlock getStrings() {
		return m_strings;
	}

	///////////////////////////////////

	private final int getAttributeOffset(int index) {
		if (m_event != START_TAG) {
			throw new IndexOutOfBoundsException("Current event is not START_TAG.");
		}
		int offset=index * 5;
		if (offset >= m_attributes.length) {
			throw new IndexOutOfBoundsException("Invalid attribute index (" + index + ").");
		}
		return offset;
	}

	private final int findAttribute(String namespace, String attribute) {
		if (m_strings == null || attribute == null) {
			return -1;
		}
		int name=m_strings.find(attribute);
		if (name == -1) {
			return -1;
		}
		int uri=(namespace != null) ?
			m_strings.find(namespace):
			-1;
		for (int o=0;o != m_attributes.length;++o) {
			if (name == m_attributes[o + ATTRIBUTE_IX_NAME] &&
				(uri == -1 || uri == m_attributes[o + ATTRIBUTE_IX_NAMESPACE_URI])) {
				return o / ATTRIBUTE_LENGHT;
			}
		}
		return -1;
	}

	private final void resetEventInfo() {
		m_event = -1;
		m_lineNumber = -1;
		m_name = -1;
		m_namespaceUri = -1;
		m_attributes = null;
		m_idAttribute = -1;
		m_classAttribute = -1;
		m_styleAttribute = -1;
	}

	private final void doNext() throws IOException {
		// Delayed initialization.
		if (m_strings == null) {
			ChunkUtil.readCheckType(m_reader, CHUNK_AXML_FILE);
			/*chunkSize*/m_reader.skipInt();
			m_strings = StringBlock.read(m_reader);
			m_namespaces.increaseDepth();
			m_operational = true;
		}

		if (m_event == END_DOCUMENT) {
			return;
		}

		int event=m_event;
		resetEventInfo();

		while (true) {
			if (m_decreaseDepth) {
				m_decreaseDepth = false;
				m_namespaces.decreaseDepth();
			}

			// Fake END_DOCUMENT event.
			if (event == END_TAG &&
				m_namespaces.getDepth() == 1 &&
				m_namespaces.getCurrentCount() == 0) {
				m_event = END_DOCUMENT;
				break;
			}

			int chunkType;
			if (event == START_DOCUMENT) {
				// Fake event, see CHUNK_XML_START_TAG handler.
				chunkType = CHUNK_XML_START_TAG;
			} else {
				chunkType = m_reader.readInt();
			}

			if (chunkType == CHUNK_RESOURCEIDS) {
				int chunkSize=m_reader.readInt();
				if (chunkSize < 8 || (chunkSize % 4) != 0) {
					throw new IOException("Invalid resource ids size (" + chunkSize + ").");
				}
				m_resourceIDs = m_reader.readIntArray(chunkSize / 4 - 2);
				continue;
			}

			if (chunkType < CHUNK_XML_FIRST || chunkType > CHUNK_XML_LAST) {
				throw new IOException("Invalid chunk type (" + chunkType + ").");
			}

			// Fake START_DOCUMENT event.
			if (chunkType == CHUNK_XML_START_TAG && event == -1) {
				m_event = START_DOCUMENT;
				break;
			}

			// Common header.
			/*chunkSize*/m_reader.skipInt();
			int lineNumber=m_reader.readInt();
			/*0xFFFFFFFF*/m_reader.skipInt();

			if (chunkType == CHUNK_XML_START_NAMESPACE ||
				chunkType == CHUNK_XML_END_NAMESPACE) {
				if (chunkType == CHUNK_XML_START_NAMESPACE) {
					int prefix=m_reader.readInt();
					int uri=m_reader.readInt();
					m_namespaces.push(prefix, uri);
				} else {
					/*prefix*/m_reader.skipInt();
					/*uri*/m_reader.skipInt();
					m_namespaces.pop();
				}
				continue;
			}

			m_lineNumber = lineNumber;

			if (chunkType == CHUNK_XML_START_TAG) {
				m_namespaceUri = m_reader.readInt();
				m_name = m_reader.readInt();
				/*flags?*/m_reader.skipInt();
				int attributeCount=m_reader.readInt();
				m_idAttribute = (attributeCount >>> 16) - 1;
				attributeCount &= 0xFFFF;
				m_classAttribute = m_reader.readInt();
				m_styleAttribute = (m_classAttribute >>> 16) - 1;
				m_classAttribute = (m_classAttribute & 0xFFFF) - 1;
				m_attributes = m_reader.readIntArray(attributeCount * ATTRIBUTE_LENGHT);
				for (int i=ATTRIBUTE_IX_VALUE_TYPE;i < m_attributes.length;) {
					m_attributes[i] = (m_attributes[i] >>> 24);
					i += ATTRIBUTE_LENGHT;
				}
				m_namespaces.increaseDepth();
				m_event = START_TAG;
				break;				
			}

			if (chunkType == CHUNK_XML_END_TAG) {
				m_namespaceUri = m_reader.readInt();
				m_name = m_reader.readInt();
				m_event = END_TAG;
				m_decreaseDepth = true;
				break;
			}

			if (chunkType == CHUNK_XML_TEXT) {
				m_name = m_reader.readInt();
				/*?*/m_reader.skipInt();
				/*?*/m_reader.skipInt();
				m_event = TEXT;
				break;				
			}
		}
	}

	/////////////////////////////////// data

	/*
	 * All values are essentially indices, e.g. m_name is
	 * an index of name in m_strings.
	 */

	private IntReader m_reader;
	private boolean m_operational=false;

	private StringBlock m_strings;
	private int[] m_resourceIDs;
	private NamespaceStack m_namespaces=new NamespaceStack();

	private boolean m_decreaseDepth;

	private int m_event;
	private int m_lineNumber;
	private int m_name;
	private int m_namespaceUri;
	private int[] m_attributes;
	private int m_idAttribute;
	private int m_classAttribute;
	private int m_styleAttribute;

	private static final String
	E_NOT_SUPPORTED	= "Method is not supported.";

	private static final int
	ATTRIBUTE_IX_NAMESPACE_URI = 0,
	ATTRIBUTE_IX_NAME = 1,
	ATTRIBUTE_IX_VALUE_STRING = 2,
	ATTRIBUTE_IX_VALUE_TYPE	= 3,
	ATTRIBUTE_IX_VALUE_DATA	= 4,
	ATTRIBUTE_LENGHT = 5;

	private static final int 
	CHUNK_AXML_FILE	= 0x00080003,
	CHUNK_RESOURCEIDS = 0x00080180,
	CHUNK_XML_FIRST	= 0x00100100,
	CHUNK_XML_START_NAMESPACE = 0x00100100,
	CHUNK_XML_END_NAMESPACE	= 0x00100101,
	CHUNK_XML_START_TAG	= 0x00100102,
	CHUNK_XML_END_TAG = 0x00100103,
	CHUNK_XML_TEXT = 0x00100104,
	CHUNK_XML_LAST = 0x00100104;
}
