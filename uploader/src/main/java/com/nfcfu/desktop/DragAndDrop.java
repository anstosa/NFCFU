package com.nfcfu.desktop;

import org.apache.http.impl.client.DefaultHttpClient;
import org.java_websocket.drafts.Draft_17;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;
import java.util.concurrent.LinkedBlockingQueue;

public class DragAndDrop implements Runnable {
    private String ipAddress = "172.17.14.16";
    static LinkedBlockingQueue<String> statuses = new LinkedBlockingQueue<String>();

    @Override
    public void run() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        } catch (UnsupportedLookAndFeelException ex) {
        }

        JFrame frame = new JFrame("Upload Files");
        DropPane dropZone = new DropPane();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(dropZone);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        while (App.nfcConnected) {
            try {
                Thread.sleep(500);
                //ipAddress = App.ips.take();
                if (ipAddress != null) {
                    dropZone.setMessage("Drag files here or");
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!App.nfcConnected) {
            dropZone.setMessage("Please connect NFC reader.");
        }
    }

    public class DropPane extends JPanel {

        private DropTarget dropTarget;
        private DropTargetHandler dropTargetHandler;
        private Point dragPoint;

        private boolean dragOver = false;
        private BufferedImage target;

        private JLabel message;
        private JLabel statusMessage;
        private FileSelect browse;

        public DropPane() {
            try {
                target = ImageIO.read(this.getClass().getResourceAsStream("/upload.png"));
                target = resizeImage(target, 263, 200);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            setLayout(new GridBagLayout());
            message = new JLabel("Waiting for phone...");
            message.setFont(message.getFont().deriveFont(Font.BOLD, 24));
            add(message);
            statusMessage = new JLabel();
            statusMessage.setFont(statusMessage.getFont().deriveFont(Font.PLAIN, 14));
            statusMessage.setVisible(false);
            add(statusMessage);
            browse = new FileSelect(this);
            browse.setVisible(false);
            add(browse);
        }

        public void setMessage(String newMessage) {
            message.setText(newMessage);
            browse.setVisible(newMessage == "Drag files here or");
        }

        public void setStatusMessage(String newStatus) {
            statusMessage.setText(newStatus);
            statusMessage.setVisible(newStatus.length() > 0);
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
            if (ipAddress == null) return;
            if (dragOver) {
                setMessage("Drop to upload");
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(0, 255, 0, 64));
                g2d.fill(new Rectangle(getWidth(), getHeight()));

                g2d.drawImage(target, getWidth() / 2 - (target.getWidth() / 2), getHeight() / 2 - (target.getHeight() / 2), this);

                g2d.dispose();
            } else if (message.getText() == "Drop to upload") {
                setMessage("Drag files here or");
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
            if (ipAddress == null) return;
            else setMessage("Uploading...");
            final DefaultHttpClient httpclient = new DefaultHttpClient();
            List<Thread> runners = new ArrayList<Thread>();

            while (ipAddress == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            for (File file : files) {
                try {
                    Thread sendRunner = new Thread(new SendFileWebSocket(file, ipAddress, new Draft_17()));
                    runners.add(sendRunner);
                    sendRunner.start();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }

            for (Thread runner : runners) {
                try {
                    runner.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            String status = null;
            while (true) {
                try {
                    Thread.sleep(500);
                    status = statuses.take();
                    if (status != null) {
                        setStatusMessage(status);
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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