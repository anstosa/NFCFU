package com.nfcfu.desktop;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TooManyListenersException;

public class DragAndDrop {

    public static void main(String[] args) {
        new DragAndDrop();
    }

    public DragAndDrop() {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException e) {
                } catch (InstantiationException e) {
                } catch (IllegalAccessException e) {
                } catch (UnsupportedLookAndFeelException ex) {
                }

                JFrame frame = new JFrame("Testing");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLayout(new BorderLayout());
                frame.add(new DropPane());
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    public class DropPane extends JPanel {

        private DropTarget dropTarget;
        private DropTargetHandler dropTargetHandler;
        private Point dragPoint;

        private boolean dragOver = false;
        private BufferedImage target;

        private JLabel message;

        public DropPane() {
            try {
                target = ImageIO.read(this.getClass().getResourceAsStream("/upload.png"));
                target = resizeImage(target, 263, 200);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            setLayout(new GridBagLayout());
            message = new JLabel();
            message.setFont(message.getFont().deriveFont(Font.BOLD, 24));
            add(message);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(400, 400);
        }

        protected DropTarget getMyDropTarget() {
            if (dropTarget == null) {
                dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, null);
            }
            return dropTarget;
        }

        protected DropTargetHandler getDropTargetHandler() {
            if (dropTargetHandler == null) {
                dropTargetHandler = new DropTargetHandler();
            }
            return dropTargetHandler;
        }

        @Override
        public void addNotify() {
            super.addNotify();
            try {
                getMyDropTarget().addDropTargetListener(getDropTargetHandler());
            } catch (TooManyListenersException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            getMyDropTarget().removeDropTargetListener(getDropTargetHandler());
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (dragOver) {
                // TODO: Give visual feedback that it will be uploaded


                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(0, 255, 0, 64));
                g2d.fill(new Rectangle(getWidth(), getHeight()));

                g2d.drawImage(target, getWidth() / 2 - (target.getWidth() / 2), getHeight() / 2 - (target.getHeight() / 2), this);

                g2d.dispose();
            }
        }

        private BufferedImage resizeImage(BufferedImage originalImage, int height, int width) {
            int type = originalImage.getType();
            BufferedImage resizedImage = new BufferedImage(width, height, type);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, width, height, null);
            g.dispose();

            return resizedImage;
        }

        protected void importFiles(final List<File> files) {
            final DefaultHttpClient httpclient = new DefaultHttpClient();

            Runnable run = new Runnable() {
                @Override
                public void run() {
                    for (File file : files) {
                        HttpPost method = new HttpPost("http://172.17.14.16:8080/");
                        MultipartEntity entity = new MultipartEntity();
                        entity.addPart("file", new FileBody(file));
                        method.setEntity(entity);

                        try {
                            HttpResponse response = httpclient.execute(method);
                            System.out.println(response.getStatusLine());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    message.setText("You dropped " + files.size() + " files");
                }
            };
            SwingUtilities.invokeLater(run);
        }

        protected class DropTargetHandler implements DropTargetListener {

            protected void processDrag(DropTargetDragEvent dtde) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                processDrag(dtde);
                SwingUtilities.invokeLater(new DragUpdate(true, dtde.getLocation()));
                repaint();
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                processDrag(dtde);
                SwingUtilities.invokeLater(new DragUpdate(true, dtde.getLocation()));
                repaint();
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                SwingUtilities.invokeLater(new DragUpdate(false, null));
                repaint();
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {

                SwingUtilities.invokeLater(new DragUpdate(false, null));

                Transferable transferable = dtde.getTransferable();
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrop(dtde.getDropAction());
                    try {
                        List<File> transferData = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (transferData != null && transferData.size() > 0) {
                            importFiles(transferData);
                            dtde.dropComplete(true);
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    dtde.rejectDrop();
                }
            }
        }

        public class DragUpdate implements Runnable {

            private boolean dragOver;
            private Point dragPoint;

            public DragUpdate(boolean dragOver, Point dragPoint) {
                this.dragOver = dragOver;
                this.dragPoint = dragPoint;
            }

            @Override
            public void run() {
                DropPane.this.dragOver = dragOver;
                DropPane.this.dragPoint = dragPoint;
                DropPane.this.repaint();
            }
        }

    }
}