package org.cltgs.ytb;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import org.schabi.newpipe.database.stream.StreamWithState;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.local.feed.FeedDatabaseManager;
import org.schabi.newpipe.local.feed.service.FeedLoadManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cz.martlin.xspf.playlist.collections.XSPFTracks;
import cz.martlin.xspf.playlist.elements.XSPFFile;
import cz.martlin.xspf.playlist.elements.XSPFTrack;
import cz.martlin.xspf.util.XSPFException;
import io.reactivex.rxjava3.core.Maybe;


public class PlaylistCreator extends ListenableWorker {
    Context appContext;
    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public PlaylistCreator(@NonNull final Context appContext,
                           @NonNull final WorkerParameters workerParams) {
        super(appContext, workerParams);
        this.appContext = appContext;
        startWork();

    }

    @NonNull
    @Override
    public ListenableFuture startWork() {
        final FeedLoadManager flm = new FeedLoadManager(appContext);
        flm.startLoading(-1L, false);
        final FeedDatabaseManager feedDatabaseManager = new FeedDatabaseManager(appContext);
        final Maybe<List<StreamWithState>> streams =
                feedDatabaseManager.getStreams(
                        -1L,
                        true,
                        true);
        final List<StreamWithState> streamList = streams.blockingGet();

        try {
            final XSPFFile file = XSPFFile.create();

            final XSPFTracks tracks = file.playlist().tracks();

            //Adding items to playlist
            for (final StreamWithState st : streamList) {
                final String title = st.getStream().getTitle();
                final String url = StreamInfo.getInfo(st.getStream().getUrl()).
                        getVideoStreams().get(1).getContent();

                final XSPFTrack track = tracks.createTrack(URI.create(url), title);
                track.setImage(URI.create(st.getStream().getThumbnailUrl()));
                track.setCreator(st.getStream().getUploader());
                tracks.add(track);
            }

            //Create file
            final File root = new File(Environment.getExternalStorageDirectory()
                    .getPath() + "/Youtube");
            root.mkdirs();
            final File f = new File(root, "Feeds.xspf");
            file.save(f);
        } catch (final XSPFException e) {
            Log.d(this.getClass().getSimpleName(), "Problem creating playlist");
        } catch (final ExtractionException e) {
            Log.d(this.getClass().getSimpleName(), "Problem getting stream");
        } catch (final IOException e) {
            Log.d(this.getClass().getSimpleName(), "Problem writing file");
        }


        final ListenableFuture fu = new ListenableFuture() {
            @Override
            public void addListener(final Runnable listener, final Executor executor) {

            }

            @Override
            public boolean cancel(final boolean b) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Object get() throws ExecutionException, InterruptedException {
                return null;
            }

            @Override
            public Object get(final long l, final TimeUnit timeUnit)
                    throws ExecutionException, InterruptedException,
                    TimeoutException {
                return null;
            }

        };
        return fu;
    }

}
