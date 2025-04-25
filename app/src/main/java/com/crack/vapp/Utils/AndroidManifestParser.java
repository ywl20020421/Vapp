package com.crack.vapp.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class AndroidManifestParser {

    private static final String ANDROID_NS_URI = "http://schemas.android.com/apk/res/android";

    // 解析XML文件并返回Document对象
    public Document parseXmlFile(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // 处理命名空间
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(xmlFile);
    }

    // 获取包含MAIN和LAUNCHER的Activity元素
    public List<Element> getLauncherActivities(Document doc) {
        List<Element> launcherActivities = new ArrayList<>();
        NodeList activityNodes = doc.getElementsByTagName("activity");

        for (int i = 0; i < activityNodes.getLength(); i++) {
            Element activity = (Element) activityNodes.item(i);
            if (hasLauncherIntentFilter(activity)) {
                launcherActivities.add(activity);
            }
        }
        return launcherActivities;
    }

    // 获取Activity的android:name属性值
    public List<String> getLauncherActivityNames(Document doc) {
        List<String> activityNames = new ArrayList<>();
        List<Element> activities = getLauncherActivities(doc);

        for (Element activity : activities) {
            String name = activity.getAttributeNS(ANDROID_NS_URI, "name");
            activityNames.add(name);
        }
        return activityNames;
    }

    // 检查是否包含启动Intent过滤器
    private boolean hasLauncherIntentFilter(Element activity) {
        NodeList intentFilters = activity.getElementsByTagName("intent-filter");

        for (int i = 0; i < intentFilters.getLength(); i++) {
            Element intentFilter = (Element) intentFilters.item(i);
            boolean hasMainAction = false;
            boolean hasLauncherCategory = false;

            NodeList children = intentFilter.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);

                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) child;
                    String tagName = element.getTagName();

                    if ("action".equals(tagName)) {
                        String nameAttr = element.getAttributeNS(ANDROID_NS_URI, "name");
                        if ("android.intent.action.MAIN".equals(nameAttr)) {
                            hasMainAction = true;
                        }
                    } else if ("category".equals(tagName)) {
                        String nameAttr = element.getAttributeNS(ANDROID_NS_URI, "name");
                        if ("android.intent.category.LAUNCHER".equals(nameAttr)) {
                            hasLauncherCategory = true;
                        }
                    }
                }
            }

            if (hasMainAction && hasLauncherCategory) {
                return true;
            }
        }
        return false;
    }

    // 示例用法
//    public static void main(String[] args) {
//        try {
//            AndroidManifestParser parser = new AndroidManifestParser();
//            Document doc = parser.parseXmlFile(new File("AndroidManifest.xml"));
//
//            // 获取所有启动Activity元素
//            List<Element> activities = parser.getLauncherActivities(doc);
//            System.out.println("Found " + activities.size() + " launcher activities");
//
////            // 获取所有启动Activity名称
////            List<String> activityNames = parser.getLauncherActivityNames(doc);
////            System.out.println("Launcher Activity Names: " + activityNames);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}