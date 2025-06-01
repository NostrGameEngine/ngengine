package org.ngengine.auth.nip46;

import org.ngengine.auth.AuthConfig;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.gui.components.NLoadingSpinner;
import org.ngengine.gui.components.NQrViewer;
import org.ngengine.gui.components.NTextInput;
import org.ngengine.gui.components.NVSpacer;
import org.ngengine.gui.components.NQrViewer.ErrorCorrectionLevel;
import org.ngengine.gui.components.containers.NColumn;
import org.ngengine.gui.win.NWindow;
import org.ngengine.gui.win.NToast.ToastType;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.nip46.BunkerUrl;
import org.ngengine.nostr4j.nip46.Nip46AppMetadata;
import org.ngengine.nostr4j.nip46.NostrconnectUrl;
import org.ngengine.nostr4j.signer.NostrNIP46Signer;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;

import com.simsilica.lemur.component.SpringGridLayout;

public class Nip46AuthWindow extends NWindow<AuthConfig> {
    private static final Logger logger = Logger.getLogger(Nip46AuthWindow.class.getName());
    protected NostrNIP46Signer signer;
    protected NostrconnectUrl nostrConnectUrl = null;

    protected Consumer<Throwable> onChallenge(String challenge, String data) {
        logger.fine("Challenge received: " + challenge + ", data: " + data);
        AsyncTask<Nip46ChallengeWindow> win = NGEPlatform.get().wrapPromise((res,rej)->{
            getManager().showWindow(Nip46ChallengeWindow.class, new String[] { challenge, data }, (r,e)->{
                if (e != null) {
                    logger.warning("Failed to open challenge window: " + e.getMessage());
                    rej.accept(e);
                } else {
                    res.accept(r);
                }
            });
        });
        return (e -> {
            getManager().runInThread(() -> {
                logger.info("Challenge closed");
                win.then(w->{
                    w.close();
                    return null;
                });
            });
        });
    }

    @Override
    protected void compose(Vector3f size, AuthConfig opt) throws Exception {

        AuthStrategy strategy = opt.getStrategy();
        if (opt.getForNpub() != null) {
            renderLoadingScreen(size, opt, "Please check your remote signer...");
        } else {
            renderAuthScreen(size, opt);
        }
        if (this.signer == null) {

            if (opt.getForNpub() != null) {
                try {
                    opt.getAuth().load( opt.getForNpub(), null).then(signer -> {
                        this.signer = (NostrNIP46Signer)signer;
                        this.signer.setChallengeHandler((v1, v2) -> {
                            try {
                                 return onChallenge(v1, v2);
                             } catch (Exception e) {
                                logger.warning("Failed to run in thread: " + e.getMessage());
                                return null;
                            }
                        }, strategy.getNip46Strategy().getChallengeTimeout());
                        // this.signer.getPublicKey()
                        // .catchException(ex->{
                        // getManager().runInThread(()->{
                        // getManager().openToast(ex);
                        // renderAuthScreen(size, opt);
                        // });
                        // })
                        // .then(pub->{

                        logger.fine("Signer is connected");
                        try {
                            auth(opt, this.signer);
                        } catch (Exception ex) {
                            getManager().showToast(ex);
                            getManager().runInThread(() -> {
                                renderAuthScreen(size, opt);
                            });
                        }
                        return null;
                    });

                } catch (Exception e) {
                    getManager().showToast(e);
                    renderAuthScreen(size, opt);
                }

                // return null;
                // });

            } else {
                NostrKeyPair appKeyPair = strategy.getNip46Strategy().getAppKeyPair();
                Nip46AppMetadata appMetadata = strategy.getNip46Strategy().getMetadata();
                this.signer = new NostrNIP46Signer(appMetadata, appKeyPair);
                this.signer.setChallengeHandler((v1, v2) -> {
                    try {
                        return onChallenge(v1, v2);
                    } catch (Exception e) {
                        logger.warning("Failed to run in thread: " + e.getMessage());
                        return null;
                    }
                }, strategy.getNip46Strategy().getChallengeTimeout());
                if (strategy.getNip46Strategy().isAllowNostrConnect()) {
                    this.signer.listen(strategy.getNip46Strategy().getRelays(), (src) -> {
                        getManager().runInThread(() -> {
                            nostrConnectUrl = src;
                            invalidate();
                        });
                    }, strategy.getNip46Strategy().getTimeout()).then((signer) -> {
                        getManager().runInThread(() -> {
                            logger.fine("Signer is connected via nostrconnect flow");
                            try {
                                auth(opt, signer);
                            } catch (Exception ex) {
                                getManager().showToast(ex);
                                renderAuthScreen(size, opt);
                            }
                        });
                        return null;
                    });
                }

            }
        }

    }

    protected void renderLoadingScreen(Vector3f size, AuthConfig opt, String msg) {
        setTitle("Authenticating");
        getContent().clearChildren();
        NColumn content = getContent().addCol();
        

        if (msg != null) {
            Label label = new Label(msg);
            label.setTextHAlignment(HAlignment.Center);
            content.addChild(label);
        }

        // LoadingSpinner spinner = new LoadingSpinner();
        // float dim = Math.min(size.x, size.y);
        // dim = dim * 0.01f;
        // if (dim < 32) dim = 32;
        // spinner.setPreferredSize(new Vector3f(dim, dim, 0));
        // content.addChild(spinner);
    }

    protected void renderAuthScreen(Vector3f size, AuthConfig opt) {
        setTitle("NIP-46 Auth");
        getContent().clearChildren();
        NColumn content = getContent().addCol();

        AuthStrategy strategy = opt.getStrategy();

        int qrSize = (int) (size.x / 3f);
        if (qrSize < 300) qrSize = 300;

        if (nostrConnectUrl != null) { // qr code (nostrconnect:// flow)
            NQrViewer qr = new NQrViewer(nostrConnectUrl.toString());
            qr.setErrorCorrectionLevel(ErrorCorrectionLevel.LOW);
            qr.setLabel("Scan with a remote signer");

            qr.setQrSize(qrSize);
            content.addChild(qr);

            NLoadingSpinner spinner = new NLoadingSpinner();
            float dim = Math.min(size.x, size.y);
            dim = dim * 0.01f;
            if (dim < 32) dim = 32;
            spinner.setPreferredSize(new Vector3f(dim, dim, 0));
            content.addChild(spinner);
        }
        if (strategy.getNip46Strategy().isAllowBunker()) {// bunker:// - nip05 flow
            Label bunkerFlowLabel = new Label("Or use the bunker:// token or nip-05 address");
            bunkerFlowLabel.setTextHAlignment(HAlignment.Center);
            Container bunkerFlow = new Container(
                    new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
            bunkerFlow.addChild(bunkerFlowLabel);

            content.addChild(bunkerFlow);

            NTextInput bunker = new NTextInput();
            bunker.setPreferredWidth(size.x / 2f);
            bunkerFlow.addChild(bunker);

            bunker.setIsSecretInput(true);

            bunkerFlow.addChild(new NVSpacer());
            bunkerFlow.addChild(new NVSpacer());

            Button nip46LoginBtn = new Button("Authenticate");
            nip46LoginBtn.setTextHAlignment(HAlignment.Center);
            nip46LoginBtn.addClickCommands((sr) -> {
                nip46LoginBtn.setEnabled(false);

                try {
                    renderLoadingScreen(size, opt, null);
                    BunkerUrl bunkerUrl = BunkerUrl.parse(bunker.getText());
                    logger.info("Authenticate to Bunker URL: " + bunkerUrl);
                    this.signer.connect(bunkerUrl).catchException(e -> {
                        getManager().runInThread(() -> {
                            getManager().showToast(e);
                            nip46LoginBtn.setEnabled(true);
                            renderAuthScreen(size, opt);
                        });
                    }).then(signer -> {
                        getManager().runInThread(() -> {
                            getManager().showToast(ToastType.INFO, "Connected to Bunker");
                            nip46LoginBtn.setEnabled(true);
                            try {
                                auth(opt, signer);
                            } catch (Exception e) {
                                getManager().showToast(e);
                            }
                        });
                        return null;
                    });
                } catch (MalformedURLException | UnsupportedEncodingException | URISyntaxException e) {
                    getManager().showToast(e);
                    renderAuthScreen(size, opt);

                }
            });

            bunkerFlow.addChild(nip46LoginBtn);
        }

    }

    protected void auth(AuthConfig opt, NostrNIP46Signer signer) throws Exception {
        AuthStrategy strategy = opt.getStrategy();

        try {
            opt.getAuth().save( signer, null).await();
        } catch (Exception e) {
            getManager().showToast(e);
        }
        
        getManager().runInThread(() -> {
            strategy.getCallback().accept(signer);
            close();
        });
    }
}