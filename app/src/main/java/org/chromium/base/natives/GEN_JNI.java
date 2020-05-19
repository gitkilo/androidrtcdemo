package org.chromium.base.natives;

import java.lang.Object;
import java.lang.String;
import java.lang.Throwable;
import javax.annotation.Generated;

@Generated("org.chromium.jni_generator.JniProcessor")
public final class GEN_JNI {
  public static boolean TESTING_ENABLED;

  public static boolean REQUIRE_MOCK;

  /**
   * org.chromium.base.AnimationFrameTimeHistogram.saveHistogram
   * @param histogramName (java.lang.String)
   * @param frameTimesMs (long[])
   * @param count (int)
   * @return (void)
   */
  public static final native void org_chromium_base_AnimationFrameTimeHistogram_saveHistogram(
      String histogramName, long[] frameTimesMs, int count);

  /**
   * org.chromium.base.ApplicationStatus.onApplicationStateChange
   * @param newState (int)
   * @return (void)
   */
  public static final native void org_chromium_base_ApplicationStatus_onApplicationStateChange(
      int newState);

  /**
   * org.chromium.base.CommandLine.appendSwitch
   * @param switchString (java.lang.String)
   * @return (void)
   */
  public static final native void org_chromium_base_CommandLine_appendSwitch(String switchString);

  /**
   * org.chromium.base.CommandLine.appendSwitchWithValue
   * @param switchString (java.lang.String)
   * @param value (java.lang.String)
   * @return (void)
   */
  public static final native void org_chromium_base_CommandLine_appendSwitchWithValue(
      String switchString, String value);

  /**
   * org.chromium.base.CommandLine.appendSwitchesAndArguments
   * @param array (java.lang.String[])
   * @return (void)
   */
  public static final native void org_chromium_base_CommandLine_appendSwitchesAndArguments(
      String[] array);

  /**
   * org.chromium.base.CommandLine.getSwitchValue
   * @param switchString (java.lang.String)
   * @return (java.lang.String)
   */
  public static final native String org_chromium_base_CommandLine_getSwitchValue(
      String switchString);

  /**
   * org.chromium.base.CommandLine.hasSwitch
   * @param switchString (java.lang.String)
   * @return (boolean)
   */
  public static final native boolean org_chromium_base_CommandLine_hasSwitch(String switchString);

  /**
   * org.chromium.base.CommandLine.init
   * @param args (java.lang.String[])
   * @return (void)
   */
  public static final native void org_chromium_base_CommandLine_init(String[] args);

  /**
   * org.chromium.base.CpuFeatures.getCoreCount
   * @return (int)
   */
  public static final native int org_chromium_base_CpuFeatures_getCoreCount();

  /**
   * org.chromium.base.CpuFeatures.getCpuFeatures
   * @return (long)
   */
  public static final native long org_chromium_base_CpuFeatures_getCpuFeatures();

  /**
   * org.chromium.base.EarlyTraceEvent.recordEarlyEvent
   * @param name (java.lang.String)
   * @param beginTimNanos (long)
   * @param endTimeNanos (long)
   * @param threadId (int)
   * @param threadDurationMillis (long)
   * @return (void)
   */
  public static final native void org_chromium_base_EarlyTraceEvent_recordEarlyEvent(String name,
      long beginTimNanos, long endTimeNanos, int threadId, long threadDurationMillis);

  /**
   * org.chromium.base.EarlyTraceEvent.recordEarlyFinishAsyncEvent
   * @param name (java.lang.String)
   * @param id (long)
   * @param timestamp (long)
   * @return (void)
   */
  public static final native void org_chromium_base_EarlyTraceEvent_recordEarlyFinishAsyncEvent(
      String name, long id, long timestamp);

  /**
   * org.chromium.base.EarlyTraceEvent.recordEarlyStartAsyncEvent
   * @param name (java.lang.String)
   * @param id (long)
   * @param timestamp (long)
   * @return (void)
   */
  public static final native void org_chromium_base_EarlyTraceEvent_recordEarlyStartAsyncEvent(
      String name, long id, long timestamp);

  /**
   * org.chromium.base.FieldTrialList.findFullName
   * @param trialName (java.lang.String)
   * @return (java.lang.String)
   */
  public static final native String org_chromium_base_FieldTrialList_findFullName(String trialName);

  /**
   * org.chromium.base.FieldTrialList.getVariationParameter
   * @param trialName (java.lang.String)
   * @param parameterKey (java.lang.String)
   * @return (java.lang.String)
   */
  public static final native String org_chromium_base_FieldTrialList_getVariationParameter(
      String trialName, String parameterKey);

  /**
   * org.chromium.base.FieldTrialList.logActiveTrials
   * @return (void)
   */
  public static final native void org_chromium_base_FieldTrialList_logActiveTrials();

  /**
   * org.chromium.base.FieldTrialList.trialExists
   * @param trialName (java.lang.String)
   * @return (boolean)
   */
  public static final native boolean org_chromium_base_FieldTrialList_trialExists(String trialName);

  /**
   * org.chromium.base.ImportantFileWriterAndroid.writeFileAtomically
   * @param fileName (java.lang.String)
   * @param data (byte[])
   * @return (boolean)
   */
  public static final native boolean org_chromium_base_ImportantFileWriterAndroid_writeFileAtomically(
      String fileName, byte[] data);

  /**
   * org.chromium.base.JavaExceptionReporter.reportJavaException
   * @param crashAfterReport (boolean)
   * @param e (java.lang.Throwable)
   * @return (void)
   */
  public static final native void org_chromium_base_JavaExceptionReporter_reportJavaException(
      boolean crashAfterReport, Throwable e);

  /**
   * org.chromium.base.JavaExceptionReporter.reportJavaStackTrace
   * @param stackTrace (java.lang.String)
   * @return (void)
   */
  public static final native void org_chromium_base_JavaExceptionReporter_reportJavaStackTrace(
      String stackTrace);

  /**
   * org.chromium.base.JavaHandlerThread.initializeThread
   * @param nativeJavaHandlerThread (long)
   * @param nativeEvent (long)
   * @return (void)
   */
  public static final native void org_chromium_base_JavaHandlerThread_initializeThread(
      long nativeJavaHandlerThread, long nativeEvent);

  /**
   * org.chromium.base.JavaHandlerThread.onLooperStopped
   * @param nativeJavaHandlerThread (long)
   * @return (void)
   */
  public static final native void org_chromium_base_JavaHandlerThread_onLooperStopped(
      long nativeJavaHandlerThread);

  /**
   * org.chromium.base.MemoryPressureListener.onMemoryPressure
   * @param pressure (int)
   * @return (void)
   */
  public static final native void org_chromium_base_MemoryPressureListener_onMemoryPressure(
      int pressure);

  /**
   * org.chromium.base.PathService.override
   * @param what (int)
   * @param path (java.lang.String)
   * @return (void)
   */
  public static final native void org_chromium_base_PathService_override(int what, String path);

  /**
   * org.chromium.base.PowerMonitor.onBatteryChargingChanged
   * @return (void)
   */
  public static final native void org_chromium_base_PowerMonitor_onBatteryChargingChanged();

  /**
   * org.chromium.base.SysUtils.logPageFaultCountToTracing
   * @return (void)
   */
  public static final native void org_chromium_base_SysUtils_logPageFaultCountToTracing();

  /**
   * org.chromium.base.TimeUtils.getTimeTicksNowUs
   * @return (long)
   */
  public static final native long org_chromium_base_TimeUtils_getTimeTicksNowUs();

  /**
   * org.chromium.base.TraceEvent.begin
   * @param name (java.lang.String)
   * @param arg (java.lang.String)
   * @return (void)
   */
  public static final native void org_chromium_base_TraceEvent_begin(String name, String arg);

  /**
   * org.chromium.base.TraceEvent.beginToplevel
   * @param target (java.lang.String)
   * @return (void)
   */
  public static final native void org_chromium_base_TraceEvent_beginToplevel(String target);

  /**
   * org.chromium.base.TraceEvent.end
   * @param name (java.lang.String)
   * @param arg (java.lang.String)
   * @return (void)
   */
  public static final native void org_chromium_base_TraceEvent_end(String name, String arg);

  /**
   * org.chromium.base.TraceEvent.endToplevel
   * @param target (java.lang.String)
   * @return (void)
   */
  public static final native void org_chromium_base_TraceEvent_endToplevel(String target);

  /**
   * org.chromium.base.TraceEvent.finishAsync
   * @param name (java.lang.String)
   * @param id (long)
   * @return (void)
   */
  public static final native void org_chromium_base_TraceEvent_finishAsync(String name, long id);

  /**
   * org.chromium.base.TraceEvent.instant
   * @param name (java.lang.String)
   * @param arg (java.lang.String)
   * @return (void)
   */
  public static final native void org_chromium_base_TraceEvent_instant(String name, String arg);

  /**
   * org.chromium.base.TraceEvent.registerEnabledObserver
   * @return (void)
   */
  public static final native void org_chromium_base_TraceEvent_registerEnabledObserver();

  /**
   * org.chromium.base.TraceEvent.startATrace
   * @return (void)
   */
  public static final native void org_chromium_base_TraceEvent_startATrace();

  /**
   * org.chromium.base.TraceEvent.startAsync
   * @param name (java.lang.String)
   * @param id (long)
   * @return (void)
   */
  public static final native void org_chromium_base_TraceEvent_startAsync(String name, long id);

  /**
   * org.chromium.base.TraceEvent.stopATrace
   * @return (void)
   */
  public static final native void org_chromium_base_TraceEvent_stopATrace();

  /**
   * org.chromium.base.library_loader.LibraryLoader.getVersionNumber
   * @return (java.lang.String)
   */
  public static final native String org_chromium_base_library_1loader_LibraryLoader_getVersionNumber(
      );

  /**
   * org.chromium.base.library_loader.LibraryLoader.libraryLoaded
   * @param processType (int)
   * @return (boolean)
   */
  public static final native boolean org_chromium_base_library_1loader_LibraryLoader_libraryLoaded(
      int processType);

  /**
   * org.chromium.base.library_loader.LibraryLoader.recordRendererLibraryLoadTime
   * @param libraryLoadTime (long)
   * @return (void)
   */
  public static final native void org_chromium_base_library_1loader_LibraryLoader_recordRendererLibraryLoadTime(
      long libraryLoadTime);

  /**
   * org.chromium.base.library_loader.LibraryPrefetcher.forkAndPrefetchNativeLibrary
   * @return (void)
   */
  public static final native void org_chromium_base_library_1loader_LibraryPrefetcher_forkAndPrefetchNativeLibrary(
      );

  /**
   * org.chromium.base.library_loader.LibraryPrefetcher.percentageOfResidentNativeLibraryCode
   * @return (int)
   */
  public static final native int org_chromium_base_library_1loader_LibraryPrefetcher_percentageOfResidentNativeLibraryCode(
      );

  /**
   * org.chromium.base.library_loader.LibraryPrefetcher.periodicallyCollectResidency
   * @return (void)
   */
  public static final native void org_chromium_base_library_1loader_LibraryPrefetcher_periodicallyCollectResidency(
      );

  /**
   * org.chromium.base.metrics.RecordHistogram.getHistogramTotalCountForTesting
   * @param name (java.lang.String)
   * @return (int)
   */
  public static final native int org_chromium_base_metrics_RecordHistogram_getHistogramTotalCountForTesting(
      String name);

  /**
   * org.chromium.base.metrics.RecordHistogram.getHistogramValueCountForTesting
   * @param name (java.lang.String)
   * @param sample (int)
   * @return (int)
   */
  public static final native int org_chromium_base_metrics_RecordHistogram_getHistogramValueCountForTesting(
      String name, int sample);

  /**
   * org.chromium.base.metrics.RecordHistogram.recordBooleanHistogram
   * @param name (java.lang.String)
   * @param key (long)
   * @param sample (boolean)
   * @return (long)
   */
  public static final native long org_chromium_base_metrics_RecordHistogram_recordBooleanHistogram(
      String name, long key, boolean sample);

  /**
   * org.chromium.base.metrics.RecordHistogram.recordCustomCountHistogram
   * @param name (java.lang.String)
   * @param key (long)
   * @param sample (int)
   * @param min (int)
   * @param max (int)
   * @param numBuckets (int)
   * @return (long)
   */
  public static final native long org_chromium_base_metrics_RecordHistogram_recordCustomCountHistogram(
      String name, long key, int sample, int min, int max, int numBuckets);

  /**
   * org.chromium.base.metrics.RecordHistogram.recordCustomTimesHistogramMilliseconds
   * @param name (java.lang.String)
   * @param key (long)
   * @param duration (int)
   * @param min (int)
   * @param max (int)
   * @param numBuckets (int)
   * @return (long)
   */
  public static final native long org_chromium_base_metrics_RecordHistogram_recordCustomTimesHistogramMilliseconds(
      String name, long key, int duration, int min, int max, int numBuckets);

  /**
   * org.chromium.base.metrics.RecordHistogram.recordEnumeratedHistogram
   * @param name (java.lang.String)
   * @param key (long)
   * @param sample (int)
   * @param boundary (int)
   * @return (long)
   */
  public static final native long org_chromium_base_metrics_RecordHistogram_recordEnumeratedHistogram(
      String name, long key, int sample, int boundary);

  /**
   * org.chromium.base.metrics.RecordHistogram.recordLinearCountHistogram
   * @param name (java.lang.String)
   * @param key (long)
   * @param sample (int)
   * @param min (int)
   * @param max (int)
   * @param numBuckets (int)
   * @return (long)
   */
  public static final native long org_chromium_base_metrics_RecordHistogram_recordLinearCountHistogram(
      String name, long key, int sample, int min, int max, int numBuckets);

  /**
   * org.chromium.base.metrics.RecordHistogram.recordSparseHistogram
   * @param name (java.lang.String)
   * @param key (long)
   * @param sample (int)
   * @return (long)
   */
  public static final native long org_chromium_base_metrics_RecordHistogram_recordSparseHistogram(
      String name, long key, int sample);

  /**
   * org.chromium.base.metrics.RecordUserAction.addActionCallbackForTesting
   * @param callback (org.chromium.base.metrics.RecordUserAction.UserActionCallback)
   * @return (long)
   */
  public static final native long org_chromium_base_metrics_RecordUserAction_addActionCallbackForTesting(
      Object callback);

  /**
   * org.chromium.base.metrics.RecordUserAction.recordUserAction
   * @param action (java.lang.String)
   * @return (void)
   */
  public static final native void org_chromium_base_metrics_RecordUserAction_recordUserAction(
      String action);

  /**
   * org.chromium.base.metrics.RecordUserAction.removeActionCallbackForTesting
   * @param callbackId (long)
   * @return (void)
   */
  public static final native void org_chromium_base_metrics_RecordUserAction_removeActionCallbackForTesting(
      long callbackId);

  /**
   * org.chromium.base.metrics.StatisticsRecorderAndroid.toJson
   * @param verbosityLevel (int)
   * @return (java.lang.String)
   */
  public static final native String org_chromium_base_metrics_StatisticsRecorderAndroid_toJson(
      int verbosityLevel);

  /**
   * org.chromium.base.process_launcher.ChildProcessService.dumpProcessStack
   * @return (void)
   */
  public static final native void org_chromium_base_process_1launcher_ChildProcessService_dumpProcessStack(
      );

  /**
   * org.chromium.base.process_launcher.ChildProcessService.exitChildProcess
   * @return (void)
   */
  public static final native void org_chromium_base_process_1launcher_ChildProcessService_exitChildProcess(
      );

  /**
   * org.chromium.base.process_launcher.ChildProcessService.registerFileDescriptors
   * @param keys (java.lang.String[])
   * @param id (int[])
   * @param fd (int[])
   * @param offset (long[])
   * @param size (long[])
   * @return (void)
   */
  public static final native void org_chromium_base_process_1launcher_ChildProcessService_registerFileDescriptors(
      String[] keys, int[] id, int[] fd, long[] offset, long[] size);

  /**
   * org.chromium.base.task.PostTask.postDelayedTask
   * @param prioritySetExplicitly (boolean)
   * @param priority (int)
   * @param mayBlock (boolean)
   * @param useThreadPool (boolean)
   * @param useCurrentThread (boolean)
   * @param extensionId (byte)
   * @param extensionData (byte[])
   * @param task (java.lang.Runnable)
   * @param delay (long)
   * @return (void)
   */
  public static final native void org_chromium_base_task_PostTask_postDelayedTask(
      boolean prioritySetExplicitly, int priority, boolean mayBlock, boolean useThreadPool,
      boolean useCurrentThread, byte extensionId, byte[] extensionData, Object task, long delay);

  /**
   * org.chromium.base.task.TaskRunnerImpl.belongsToCurrentThread
   * @param nativeTaskRunnerAndroid (long)
   * @return (boolean)
   */
  public static final native boolean org_chromium_base_task_TaskRunnerImpl_belongsToCurrentThread(
      long nativeTaskRunnerAndroid);

  /**
   * org.chromium.base.task.TaskRunnerImpl.destroy
   * @param nativeTaskRunnerAndroid (long)
   * @return (void)
   */
  public static final native void org_chromium_base_task_TaskRunnerImpl_destroy(
      long nativeTaskRunnerAndroid);

  /**
   * org.chromium.base.task.TaskRunnerImpl.init
   * @param taskRunnerType (int)
   * @param prioritySetExplicitly (boolean)
   * @param priority (int)
   * @param mayBlock (boolean)
   * @param useThreadPool (boolean)
   * @param useCurrentThread (boolean)
   * @param extensionId (byte)
   * @param extensionData (byte[])
   * @return (long)
   */
  public static final native long org_chromium_base_task_TaskRunnerImpl_init(int taskRunnerType,
      boolean prioritySetExplicitly, int priority, boolean mayBlock, boolean useThreadPool,
      boolean useCurrentThread, byte extensionId, byte[] extensionData);

  /**
   * org.chromium.base.task.TaskRunnerImpl.postDelayedTask
   * @param nativeTaskRunnerAndroid (long)
   * @param task (java.lang.Runnable)
   * @param delay (long)
   * @return (void)
   */
  public static final native void org_chromium_base_task_TaskRunnerImpl_postDelayedTask(
      long nativeTaskRunnerAndroid, Object task, long delay);
}
