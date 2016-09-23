package org.dcm4chee.web.war.fs;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.dcm4chee.archive.entity.FileSystem;
import org.dcm4chee.archive.util.JNDIUtils;
import org.dcm4chee.web.dao.fs.FileSystemHomeLocal;

public class FileSystemPage extends Panel {

    private static final long serialVersionUID = 1L;

    private List<FileSystem> list;

    public FileSystemPage(final String id) {
        super(id);
        
        try {
            FileSystemHomeLocal dao = (FileSystemHomeLocal)
                    JNDIUtils.lookup(FileSystemHomeLocal.JNDI_NAME);
            list = dao.findAll();
        } catch (Exception e) {
            list = Collections.emptyList();
        }
        
        add(new PropertyListView<Object>("list", list) {

            private static final long serialVersionUID = 1L;

            protected void populateItem(ListItem<Object> item) {
                FileSystem fs = (FileSystem) item.getModelObject();
                item.add(new Label("pk"));
                item.add(new Label("directoryPath"));
                item.add(new Label("groupID"));
                item.add(new Label("retrieveAET", fs.getRetrieveAET()));
                item.add(new Label("availability"));
                item.add(new Label("status"));
                item.add(new Label("nextFileSystem.directoryPath"));
            }
        });
    }

    public static String getModuleName() {
        return "filesystem";
    }
}
