package org.cltgs.ytb;

import static org.schabi.newpipe.util.ListHelper.getUrlAndNonTorrentStreams;

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
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.local.feed.FeedDatabaseManager;
import org.schabi.newpipe.local.feed.service.FeedLoadManager;
import org.schabi.newpipe.util.ListHelper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import cz.martlin.xspf.playlist.collections.XSPFTracks;
import cz.martlin.xspf.playlist.elements.XSPFFile;
import cz.martlin.xspf.playlist.elements.XSPFTrack;
import cz.martlin.xspf.util.XSPFException;
import io.reactivex.rxjava3.core.Maybe;


public class PlaylistStreamCreator extends ListenableWorker {
    Context appContext;
    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public PlaylistStreamCreator(@NonNull final Context appContext,
                                 @NonNull final WorkerParameters workerParams) {
        super(appContext, workerParams);
        this.appContext = appContext;
        startWork();

    }

    @NonNull
    @Override
    public ListenableFuture startWork() {
        //Get streams (feeds)
        final FeedLoadManager flm = new FeedLoadManager(appContext);
        flm.startLoading(-1L, false).blockingGet();
        final FeedDatabaseManager feedDatabaseManager =
                new FeedDatabaseManager(appContext);
        final Maybe<List<StreamWithState>> streams =
                feedDatabaseManager.getStreams(
                        -1L,
                        true,
                        true);
        final List<StreamWithState> streamList = streams.blockingGet();
        createStreamList(appContext, streamList, "Feeds.xspf");

        //Get streams (hot!)

        return new ListenableFuture() {
            @Override
            public void addListener(@NonNull final Runnable listener,
                                    @NonNull final Executor executor) {

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
    }

    private void createStreamList(final Context cxt,
                                  final List<StreamWithState> streamListFull,
                                  final String nameFile) {
        //Create playlist
        try {
            final XSPFFile file = XSPFFile.create();

            final XSPFTracks tracks = file.playlist().tracks();

            //Adding items to playlist
            final List<StreamWithState> streamList = streamListFull
                    .stream().limit(5).collect(Collectors.toList());
            for (final StreamWithState st : streamList) {
                final String title = st.getStream().getTitle();
                final List<VideoStream> videoStreamsForExternalPlayers =
                        ListHelper.getSortedStreamVideosList(
                                cxt,
                                getUrlAndNonTorrentStreams(

                                        StreamInfo.getInfo(
                                                st.getStream().getUrl()
                                        ).getVideoStreams()
                                ),
                                null,
                                false,
                                false);
                final int index = ListHelper.getDefaultResolutionIndex(cxt,
                        videoStreamsForExternalPlayers);
                final String url = StreamInfo.getInfo(st.getStream().getUrl()).
                        getVideoStreams().get(index).getContent();

                final XSPFTrack track = tracks.createTrack(URI.create(url), title);
                track.setImage(URI.create(st.getStream().getThumbnailUrl()));
                track.setCreator(st.getStream().getUploader());
                tracks.add(track);
            }

            //Create file
            final File root = new File(Environment.getExternalStorageDirectory()
                    .getPath() + "/Youtube");
            root.mkdirs();
            final File f = new File(root, nameFile);
            file.save(f);
        } catch (final XSPFException e) {
            Log.d(this.getClass().getSimpleName(), "Problem creating playlist");
        } catch (final ExtractionException e) {
            Log.d(this.getClass().getSimpleName(), "Problem getting stream");
        } catch (final IOException e) {
            Log.d(this.getClass().getSimpleName(), "Problem writing file");
        }
    }

}
