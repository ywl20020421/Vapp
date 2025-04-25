package axml.xml;

import java.lang.reflect.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.util.regex.*;
import org.xmlpull.v1.*;
import android.text.*;
import android.graphics.*;

public abstract class Chunk<H extends Chunk.Header> {

    public abstract class Header {
        public short type;
        public short headerSize;
        public int size;

        public Header(ChunkType ct) {
            type = ct.type;
            headerSize = ct.headerSize;
        }

        public void write(IntWriter w) throws IOException {
            w.write(type);
            w.write(headerSize);
            w.write(size);
            writeEx(w);
        }

        public abstract void writeEx(IntWriter w) throws IOException;
    }

    public abstract class NodeHeader extends Header {

        public int lineNo=1;
        public int comment=-1;

        public NodeHeader(ChunkType ct) {
            super(ct);
            headerSize = 0x10;
        }

        @Override
        public void write(IntWriter w) throws IOException {
            w.write(type);
            w.write(headerSize);
            w.write(size);
            w.write(lineNo);
            w.write(comment);
            writeEx(w);
        }

        @Override
        public void writeEx(IntWriter w) throws IOException {

        }
    }

    public class EmptyHeader extends Header {
        public EmptyHeader() {
            super(ChunkType.Null);
        }
        @Override
        public void writeEx(IntWriter w) throws IOException {}
        @Override
        public void write(IntWriter w) throws IOException {}
    }


    public Chunk(Chunk parent) {
        this.parent = parent;
        try {
            Class<H> t = (Class<H>)((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            Constructor[] cs=t.getConstructors();
            for (Constructor c:cs) {
                Type[] ts=c.getParameterTypes();
                if (ts.length == 1 && Chunk.class.isAssignableFrom((Class<?>)ts[0]))
                    header = (H) c.newInstance(this);
            }
        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    protected Context context;
    private Chunk parent;
    public H header;

    public void write(IntWriter w) throws IOException {
        int pos=w.getPos();
        calc();
        header.write(w);
        writeEx(w);
        assert w.getPos() - pos == header.size:(w.getPos() - pos) + " instead of " + header.size + " bytes were written:" + getClass().getName();
    }

    public Chunk getParent() {
        return parent;
    }

    public Context getContext() {
        return this.context != null ? this.context : getParent().getContext();
    }

    private boolean isCalculated=false;
    public int calc() {
        if (!isCalculated) {
            preWrite();
            isCalculated = true;
        }
        return header.size;
    }

    private XmlChunk root;
    public XmlChunk root() {
        if (root != null) return root;
        return getParent().root();
    }
    public int stringIndex(String namespace, String s) {
        return stringPool().stringIndex(namespace, s);
    }

    private StringPoolChunk stringPool=null;
    public StringPoolChunk stringPool() {
        return root().stringPool;
    }

    public ReferenceResolver getReferenceResolver() {
        return root().getReferenceResolver();
    }

    public void preWrite() {}
    public abstract void writeEx(IntWriter w) throws IOException;

}

enum ChunkType {
    Null(0x0000, -1),
    StringPool(0x0001, 28),
    Table(0x0002, -1),
    Xml(0x0003, 8),

    // chunk types in resxmlType
    XmlFirstChunk(0x0100, -1),
    XmlStartNamespace(0x0100, 0x10),
    XmlEndNamespace(0x0101, 0x10),
    XmlStartElement(0x0102, 0x10),
    XmlEndElement(0x0103, 0x10),
    XmlCdata(0x0104, -1),
    XmlLastChunk(0x017f, -1),
    // this Contains a uint32t array mapping strings in the string
    // pool Back to resource identifiers.  it is optional.
    XmlResourceMap(0x0180, 8),

    // chunk types in restableType
    TablePackage(0x0200, -1),
    TableType(0x0201, -1),
    TableTypeSpec(0x0202, -1);

    public final short type;
    public final short headerSize;

    ChunkType(int type, int headerSize) {
		this.type = (short)type;
		this.headerSize = (short)headerSize;
    }
};

class EndNameSpaceChunk extends Chunk<EndNameSpaceChunk.H> {
    public class H extends NodeHeader {
        public H() {
			super(ChunkType.XmlEndNamespace);
			size = 0x18;
        }
    }
    public StartNameSpaceChunk start;
    public EndNameSpaceChunk(Chunk parent, StartNameSpaceChunk start) {
		super(parent);
		this.start = start;
    }
    @Override
    public void writeEx(IntWriter w) throws IOException {
		start.writeEx(w);
    }
}

class EndTagChunk extends Chunk<EndTagChunk.H> {
    public class H extends NodeHeader {

        public H() {
			super(ChunkType.XmlEndElement);
			this.size = 24;
        }
    }

    public StartTagChunk start;
    public EndTagChunk(Chunk parent, StartTagChunk start) {
		super(parent);
		this.start = start;
    }

    @Override
    public void writeEx(IntWriter w) throws IOException {
		w.write(stringIndex(null, start.namespace));
		w.write(stringIndex(null, start.name));
    }
}

class ResourceMapChunk extends Chunk<ResourceMapChunk.H> {

    public class H extends Header {

        public H() {
			super(ChunkType.XmlResourceMap);
        }

        @Override
        public void writeEx(IntWriter w) throws IOException {

        }
    }

    public ResourceMapChunk(Chunk parent) {
		super(parent);
    }

    private LinkedList<Integer> ids;

    @Override
    public void preWrite() {
		List<StringPoolChunk.RawString> rss=stringPool().rawStrings;
		ids = new LinkedList<Integer>();
		for (StringPoolChunk.RawString r:rss) {
			if (r.origin.id >= 0) {
				ids.add(r.origin.id);
			} else {
				break;
			}
		}
		header.size = ids.size() * 4 + header.headerSize;
    }

    @Override
    public void writeEx(IntWriter w) throws IOException {
		for (int i:ids) w.write(i);
    }
}

class StartNameSpaceChunk extends Chunk<StartNameSpaceChunk.H> {

    public StartNameSpaceChunk(Chunk parent) {
		super(parent);
    }

    public class H extends NodeHeader {
        public H() {
			super(ChunkType.XmlStartNamespace);
			size = 0x18;
        }
    }

    public String prefix;
    public String uri;

    @Override
    public void writeEx(IntWriter w) throws IOException {
		w.write(stringIndex(null, prefix));
		w.write(stringIndex(null, uri));
    }
}

class StartTagChunk extends Chunk<StartTagChunk.H> {
    public class H extends NodeHeader {

        public H() {
			super(ChunkType.XmlStartElement);
        }
    }

    public String name;
    public String prefix;
    public String namespace;
    public short attrStart=20;
    public short attrSize=20;
    public short idIndex=0;
    public short styleIndex=0;
    public short classIndex=0;
    public LinkedList<AttrChunk> attrs=new LinkedList<AttrChunk>();
    public List<StartNameSpaceChunk> startNameSpace=new Stack<StartNameSpaceChunk>();

    public StartTagChunk(Chunk parent, XmlPullParser p) throws XmlPullParserException {
		super(parent);
		name = p.getName();
		stringPool().addString(name);
		prefix = p.getPrefix();
		namespace = p.getNamespace();
		int ac = p.getAttributeCount();
		for (short i = 0; i < ac; ++i) {
			String prefix = p.getAttributePrefix(i);
			String namespace = p.getAttributeNamespace(i);
			String name = p.getAttributeName(i);
			String val = p.getAttributeValue(i);
			AttrChunk attr = new AttrChunk(this);
			attr.prefix = prefix;
			attr.namespace = namespace;
			attr.rawValue = val;
			attr.name = name;
			stringPool().addString(namespace, name);
			attrs.add(attr);
			if ("id".equals(name) && "http://schemas.android.com/apk/res/android".equals(namespace)) {
				idIndex = i;
			} else if (prefix == null && "style".equals(name)) {
				styleIndex = i;
			} else if (prefix == null && "class".equals(name)) {
				classIndex = i;
			}
		}
		int nsStart = p.getNamespaceCount(p.getDepth() - 1);
		int nsEnd = p.getNamespaceCount(p.getDepth());
		for (int i = nsStart; i < nsEnd; i++) {
			StartNameSpaceChunk snc=new StartNameSpaceChunk(parent);
			snc.prefix = p.getNamespacePrefix(i);
			stringPool().addString(null, snc.prefix);
			snc.uri = p.getNamespaceUri(i);
			stringPool().addString(null, snc.uri);
			startNameSpace.add(snc);
		}
    }

    @Override
    public void preWrite() {
		for (AttrChunk a:attrs) a.calc();
		header.size = 36 + 20 * attrs.size();
    }

    @Override
    public void writeEx(IntWriter w) throws IOException {
		w.write(stringIndex(null, namespace));
		w.write(stringIndex(null, name));
		w.write(attrStart);
		w.write(attrSize);
		w.write((short)attrs.size());
		w.write(idIndex);
		w.write(classIndex);
		w.write(styleIndex);
		for (AttrChunk a:attrs) {
			a.write(w);
		}
    }
}

class StringPoolChunk extends Chunk<StringPoolChunk.H> {

    public StringPoolChunk(Chunk parent) {
		super(parent);
    }

    public class H extends Header {
        public int stringCount;
        public int styleCount;
        public int flags;
        public int stringPoolOffset;
        public int stylePoolOffset;

        public H() {super(ChunkType.StringPool);}

        @Override
        public void writeEx(IntWriter w) throws IOException {
			w.write(stringCount);
			w.write(styleCount);
			w.write(flags);
			w.write(stringPoolOffset);
			w.write(stylePoolOffset);
        }
    }

    public static class RawString {

        StringItem origin;
        int byteLength;
        char[] cdata;
        byte[] bdata;

        int length() {
			if (cdata != null)return cdata.length;
			return origin.string.length();
        };

        int padding() {
			if (cdata != null) {
				return (cdata.length * 2 + 4) & 2;
			} else {
				//return (4-((bdata.length+3)&3))&3;
				return 0;
			}
        }

        int size() {
			if (cdata != null) {
				return cdata.length * 2 + 4 + padding();
			} else {
				return bdata.length + 3 + padding();
			}
        }

        void write(IntWriter w) throws IOException {
			if (cdata != null) {
				int pos=w.getPos();
				w.write((short)length());
				for (char c:cdata) w.write(c);
				w.write((short)0);
				if (padding() == 2)w.write((short)0);
				assert size() == w.getPos() - pos:size() + "," + (w.getPos() - pos);
			} else {
				int pos=w.getPos();
				w.write((byte)length());
				w.write((byte)bdata.length);
				for (byte c:bdata) w.write(c);
				w.write((byte)0);
				int p=padding();
				for (int i=0;i < p;++i) w.write((byte)0);
				assert size() == w.getPos() - pos:size() + "," + (w.getPos() - pos);
			}
        }
    }

    public enum Encoding {UNICODE,UTF8}
    public int[] stringsOffset;
    public int[] stylesOffset;
    public ArrayList<RawString> rawStrings;

    public Encoding encoding = AxmlUtil.Config.encoding;

    @Override
    public void preWrite() {
		rawStrings = new ArrayList<RawString>();
		LinkedList<Integer> offsets=new LinkedList<Integer>();
		int off=0;
		int i=0;
		if (encoding == Encoding.UNICODE) {
			for (LinkedList<StringItem> ss: map.values()) {
				for (StringItem s:ss) {
					RawString r = new RawString();
					r.cdata = s.string.toCharArray();
					r.origin = s;
					rawStrings.add(r);
				}
			}
		} else {
			for (LinkedList<StringItem> ss: map.values()) {
				for (StringItem s:ss) {
					RawString r = new RawString();
					try {
						r.bdata = s.string.getBytes("UTF-8");
					} catch (Exception e) {}
					r.origin = s;
					rawStrings.add(r);
				}
			}

		}
		Collections.sort(rawStrings, new Comparator<RawString>() {

				public int compare(RawString lhs, RawString rhs) {
					int l = lhs.origin.id;
					int r = rhs.origin.id;
					if (l == -1) l = Integer.MAX_VALUE;
					if (r == -1) r = Integer.MAX_VALUE;
					return l - r;
				}
			});
		for (RawString r:rawStrings) {
			offsets.add(off);
			off += r.size();
		}
		header.stringCount = rawStrings.size();
		header.styleCount = 0;
		header.size = off + header.headerSize + header.stringCount * 4 + header.styleCount * 4;
		header.stringPoolOffset = offsets.size() * 4 + header.headerSize;
		header.stylePoolOffset = 0;
		stringsOffset = new int[offsets.size()];
		if (encoding == Encoding.UTF8) header.flags |= 0x100;
		i = 0;
		for (int x:offsets) stringsOffset[i++] = x;
		stylesOffset = new int[0];
    }

    @Override
    public void writeEx(IntWriter w) throws IOException {
		for (int i:stringsOffset) w.write(i);
		for (int i:stylesOffset) w.write(i);
		for (RawString r:rawStrings) r.write(w);
		//TODO styles;
    }


    public class StringItem {
        public String namespace;
        public String string;
        public int id=-1;;

        public StringItem(String s) {
			string = s;
			namespace = null;
        }

        public StringItem(String namespace, String s) {
			string = s;
			this.namespace = namespace;
			genId();
        }

        public void setNamespace(String namespace) {
			this.namespace = namespace;
			genId();
        }

        public void genId() {
			if (namespace == null) return;
			String pkg="http://schemas.android.com/apk/res-auto".equals(namespace) ?getContext().getPackageName():
				namespace.startsWith("http://schemas.android.com/apk/res/") ?namespace.substring("http://schemas.android.com/apk/res/".length()): null;
			if (pkg == null) return;
			id = getContext().getResources().getIdentifier(string, "attr", pkg);
        }
    }

    private HashMap<String,LinkedList<StringItem>> map=new HashMap<String,LinkedList<StringItem>>();
    private String preHandleString(String s) {
		return s;
    }
    public void addString(String s) {
		s = preHandleString(s);
		LinkedList<StringItem> list=map.get(s);
		if (list == null) map.put(s, list = new LinkedList<StringItem>());
		if (!list.isEmpty()) return;
		StringItem item=new StringItem(s);
		list.add(item);
    }

    public void addString(String namespace, String s) {
		namespace = preHandleString(namespace);
		s = preHandleString(s);
		LinkedList<StringItem> list=map.get(s);
		if (list == null) map.put(s, list = new LinkedList<StringItem>());
		for (StringItem e:list) if (e.namespace == null || e.namespace.equals(namespace)) {
				e.setNamespace(namespace);
				return;
			}
		StringItem item=new StringItem(namespace, s);
		list.add(item);
    }

    @Override
    public int stringIndex(String namespace, String s) {
		namespace = preHandleString(namespace);
		s = preHandleString(s);
		if (TextUtils.isEmpty(s)) return -1;
		int l=rawStrings.size();
		for (int i=0;i < l;++i) {
			StringItem item=rawStrings.get(i).origin;
			if (s.equals(item.string) && (TextUtils.isEmpty(namespace) || namespace.equals(item.namespace))) return i;
		}
		throw new RuntimeException("String: '" + s + "' not found");
		//return -1;
    }
}

class TagChunk extends Chunk<Chunk.EmptyHeader> {



    public List<StartNameSpaceChunk> startNameSpace;
    public StartTagChunk startTag;
    public List<TagChunk> content=new LinkedList<TagChunk>();
    public EndTagChunk endTag;
    public List<EndNameSpaceChunk> endNameSpace;

    public TagChunk(Chunk parent, XmlPullParser p) throws XmlPullParserException {
		super(parent);
		if (parent instanceof TagChunk) {
			((TagChunk) parent).content.add(this);
		} else if (parent instanceof XmlChunk) {
			((XmlChunk) parent).content = this;
		} else {
			throw new IllegalArgumentException("parent must be XmlChunk or TagChunk");
		}
		startTag = new StartTagChunk(this, p);
		endTag = new EndTagChunk(this, startTag);
		startNameSpace = startTag.startNameSpace;
		endNameSpace = new LinkedList<EndNameSpaceChunk>();
		for (StartNameSpaceChunk c:startNameSpace) endNameSpace.add(new EndNameSpaceChunk(this, c));
		endTag.header.lineNo = startTag.header.lineNo = p.getLineNumber();
    }

    @Override
    public void preWrite() {
		int sum=0;
		for (StartNameSpaceChunk e:startNameSpace) sum += e.calc();
		for (EndNameSpaceChunk e:endNameSpace) sum += e.calc();
		sum += startTag.calc();
		sum += endTag.calc();
		for (TagChunk c:content) sum += c.calc();
		header.size = sum;
    }

    @Override
    public void writeEx(IntWriter w) throws IOException {
		for (StartNameSpaceChunk c:startNameSpace) c.write(w);
		startTag.write(w);
		for (TagChunk c:content) c.write(w);
		endTag.write(w);
		for (EndNameSpaceChunk c:endNameSpace) c.write(w);
    }
}

class ValueChunk extends Chunk<Chunk.EmptyHeader> {

    class ValPair {
        int pos;
        String val;

        public ValPair(Matcher m) {
			int c = m.groupCount();
			for (int i = 1; i <= c; ++i) {
				String s = m.group(i);
				if (s == null || s.isEmpty()) continue;
				pos = i;
				val = s;
				return;
			}
			pos = -1;
			val = m.group();
        }
    }

    private AttrChunk attrChunk;
    private String realString;
    short size = 8;
    byte res0 = 0;
    byte type = -1;
    int data = -1;

    Pattern explicitType = Pattern.compile("!(?:(\\w+)!)?(.*)");
    Pattern types = Pattern.compile(("^(?:" +
									"(@null)" +
									"|(@\\+?(?:\\w+:)?\\w+/\\w+|@(?:\\w+:)?[0-9a-zA-Z]+)" +
									"|(true|false)" +
									"|([-+]?\\d+)" +
									"|(0x[0-9a-zA-Z]+)" +
									"|([-+]?\\d+(?:\\.\\d+)?)" +
									"|([-+]?\\d+(?:\\.\\d+)?(?:dp|dip|in|px|sp|pt|mm))" +
									"|([-+]?\\d+(?:\\.\\d+)?(?:%))" +
									"|(\\#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8}))" +
									"|(match_parent|wrap_content|fill_parent)" +
									")$").replaceAll("\\s+", ""));

    public ValueChunk(AttrChunk parent) {
		super(parent);
		header.size = 8;
		this.attrChunk = parent;
    }

    @Override
    public void preWrite() {
		evaluate();
    }

    @Override
    public void writeEx(IntWriter w) throws IOException {
		w.write(size);
		w.write(res0);
		if (type == ValueType.STRING) {
			data = stringIndex(null, realString);
		}
		w.write(type);
		w.write(data);
    }

    public int evalcomplex(String val) {
		int unit;
		int radix;
		int base;
		String num;

		if (val.endsWith("%")) {
			num = val.substring(0, val.length() - 1);
			unit = ComplexConsts.UNIT_FRACTION;
		} else if (val.endsWith("dp")) {
			unit = ComplexConsts.UNIT_DIP;
			num = val.substring(0, val.length() - 2);
		} else if (val.endsWith("dip")) {
			unit = ComplexConsts.UNIT_DIP;
			num = val.substring(0, val.length() - 3);
		} else if (val.endsWith("sp")) {
			unit = ComplexConsts.UNIT_SP;
			num = val.substring(0, val.length() - 2);
		} else if (val.endsWith("px")) {
			unit = ComplexConsts.UNIT_PX;
			num = val.substring(0, val.length() - 2);
		} else if (val.endsWith("pt")) {
			unit = ComplexConsts.UNIT_PT;
			num = val.substring(0, val.length() - 2);
		} else if (val.endsWith("in")) {
			unit = ComplexConsts.UNIT_IN;
			num = val.substring(0, val.length() - 2);
		} else if (val.endsWith("mm")) {
			unit = ComplexConsts.UNIT_MM;
			num = val.substring(0, val.length() - 2);
		} else {
			throw new RuntimeException("invalid unit");
		}
		double f = Double.parseDouble(num);
		if (f < 1 && f > -1) {
			base = (int) (f * (1 << 23));
			radix = ComplexConsts.RADIX_0p23;
		} else if (f < 0x100 && f > -0x100) {
			base = (int) (f * (1 << 15));
			radix = ComplexConsts.RADIX_8p15;
		} else if (f < 0x10000 && f > -0x10000) {
			base = (int) (f * (1 << 7));
			radix = ComplexConsts.RADIX_16p7;
		} else {
			base = (int) f;
			radix = ComplexConsts.RADIX_23p0;
		}
		return (base << 8) | (radix << 4) | unit;
    }

    public void evaluate() {
		Matcher m = explicitType.matcher(attrChunk.rawValue);
		if (m.find()) {
			String t = m.group(1);
			String v = m.group(2);
			if (t == null || t.isEmpty() || t.equals("string") || t.equals("str")) {
				type = ValueType.STRING;
				realString = v;
				stringPool().addString(realString);
				//data = stringIndex(null, v);
			} else {
				//TODO resolve other type
				throw new RuntimeException();
			}
		} else {
			m = types.matcher(attrChunk.rawValue);
			if (m.find()) {
				ValPair vp = new ValPair(m);
				switch (vp.pos) {
					case 1:
						type = ValueType.NULL;
						data = 0;
						break;
					case 2:
						type = ValueType.REFERENCE;
						data = getReferenceResolver().resolve(this, vp.val);
						break;
					case 3:
						type = ValueType.INT_BOOLEAN;
						data = "true".equalsIgnoreCase(vp.val) ? 1 : 0;
						break;
					case 4:
						type = ValueType.INT_DEC;
						data = Integer.parseInt(vp.val);
						break;
					case 5:
						type = ValueType.INT_HEX;
						data = Integer.parseInt(vp.val.substring(2), 16);
						break;
					case 6:
						type = ValueType.FLOAT;
						data = Float.floatToIntBits(Float.parseFloat(vp.val));
						break;
					case 7:
						type = ValueType.DIMENSION;
						data = evalcomplex(vp.val);
						break;
					case 8:
						type = ValueType.FRACTION;
						data = evalcomplex(vp.val);
						break;
					case 9:
						type = ValueType.INT_COLOR_ARGB8;
						data = Color.parseColor(vp.val);
						break;
					case 10:
						type = ValueType.INT_DEC;
						data = "wrap_content".equalsIgnoreCase(vp.val) ? -2 : -1;
						break;
					default:
						type = ValueType.STRING;
						realString = vp.val;
						stringPool().addString(realString);
						//data = stringIndex(null, attrChunk.rawValue);
						break;
				}
			} else {
				type = ValueType.STRING;
				realString = attrChunk.rawValue;
				stringPool().addString(realString);
				//data = stringIndex(null, attrChunk.rawValue);
			}
		}
    }
}

class XmlChunk extends Chunk<XmlChunk.H> {
    public XmlChunk() {
		super(null);
		this.context = new Context();
    }

    public class H extends Header {

        public H() {
			super(ChunkType.Xml);
        }

        @Override
        public void writeEx(IntWriter w) throws IOException {

        }
    }
    public StringPoolChunk stringPool=new StringPoolChunk(this);
    public ResourceMapChunk resourceMap=new ResourceMapChunk(this);
    public TagChunk content;

    @Override
    public void preWrite() {
		header.size = header.headerSize + content.calc() + stringPool.calc() + resourceMap.calc();
    }

    @Override
    public void writeEx(IntWriter w) throws IOException {
		stringPool.write(w);
		resourceMap.write(w);
		content.write(w);
    }

    @Override
    public XmlChunk root() {
		return this;
    }

    private ReferenceResolver referenceResolver;
    @Override
    public ReferenceResolver getReferenceResolver() {
		if (referenceResolver == null) referenceResolver = new DefaultReferenceResolver();
		return referenceResolver;
    }
}

class AttrChunk extends Chunk<Chunk.EmptyHeader> {
    private StartTagChunk startTagChunk;
    public String prefix;
    public String name;
    public String namespace;
    public String rawValue;

    public AttrChunk(StartTagChunk startTagChunk) {
        super(startTagChunk);
        this.startTagChunk = startTagChunk;
        header.size = 20;
    }
    public ValueChunk value = new ValueChunk(this);
    @Override
    public void preWrite() {
        value.calc();
    }
    @Override
    public void writeEx(IntWriter w) throws IOException {
        w.write(startTagChunk.stringIndex(null, namespace));
        w.write(startTagChunk.stringIndex(namespace, name));
        //w.write(-1);
        if (value.type == ValueType.STRING)
            w.write(startTagChunk.stringIndex(null, rawValue));
        else
            w.write(-1);
        value.write(w);
    }
}

class IntWriter {
    private OutputStream os;
    public boolean bigEndian=false;
    private int pos=0;

    public IntWriter(OutputStream os) {
		this.os = os;
    }

    public void write(byte b) throws IOException {
		os.write(b);
		pos += 1;
    }

    public void write(short s) throws IOException {
		if (!bigEndian) {
			os.write(s & 0xff);
			os.write((s >>> 8) & 0xff);
		} else {
			os.write((s >>> 8) & 0xff);
			os.write(s & 0xff);
		}
		pos += 2;
    }

    public void write(char x) throws IOException {
		write((short)x);
    }

    public void write(int x) throws IOException {
		if (!bigEndian) {
			os.write(x & 0xff);
			x >>>= 8;
			os.write(x & 0xff);
			x >>>= 8;
			os.write(x & 0xff);
			x >>>= 8;
			os.write(x & 0xff);
		} else {
			throw new RuntimeException();
		}
		pos += 4;
    }

    public void writePlaceHolder(int len, String name) throws IOException {
		os.write(new byte[len]);
    }

    public void close() throws IOException {
		os.close();
    }

    public int getPos() {
		return pos;
    }
}

class Context {
    private Resources resources = new Resources();
    public Resources getResources() {
		return resources;
    }

    public String getPackageName() {
		return "axml.xml";
    }
}
