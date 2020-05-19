package org.chromium.base.metrics;

import java.lang.Override;
import java.lang.String;
import javax.annotation.Generated;
import org.chromium.base.JniStaticTestMocker;
import org.chromium.base.annotations.CheckDiscard;
import org.chromium.base.annotations.MainDex;
import org.chromium.base.natives.GEN_JNI;

@Generated("org.chromium.jni_generator.JniProcessor")
@MainDex
@CheckDiscard("crbug.com/993421")
public final class RecordHistogramJni implements RecordHistogram.Natives {
  private static RecordHistogram.Natives testInstance;

  public static final JniStaticTestMocker<RecordHistogram.Natives> TEST_HOOKS = new org.chromium.base.JniStaticTestMocker<org.chromium.base.metrics.RecordHistogram.Natives>() {
    @java.lang.Override
    public void setInstanceForTesting(org.chromium.base.metrics.RecordHistogram.Natives instance) {
      testInstance = instance;
    }
  };

  @Override
  public long recordCustomTimesHistogramMilliseconds(String name, long key, int duration, int min,
      int max, int numBuckets) {
    return (long)GEN_JNI.org_chromium_base_metrics_RecordHistogram_recordCustomTimesHistogramMilliseconds(name, key, duration, min, max, numBuckets);
  }

  @Override
  public long recordBooleanHistogram(String name, long key, boolean sample) {
    return (long)GEN_JNI.org_chromium_base_metrics_RecordHistogram_recordBooleanHistogram(name, key, sample);
  }

  @Override
  public long recordEnumeratedHistogram(String name, long key, int sample, int boundary) {
    return (long)GEN_JNI.org_chromium_base_metrics_RecordHistogram_recordEnumeratedHistogram(name, key, sample, boundary);
  }

  @Override
  public long recordCustomCountHistogram(String name, long key, int sample, int min, int max,
      int numBuckets) {
    return (long)GEN_JNI.org_chromium_base_metrics_RecordHistogram_recordCustomCountHistogram(name, key, sample, min, max, numBuckets);
  }

  @Override
  public long recordLinearCountHistogram(String name, long key, int sample, int min, int max,
      int numBuckets) {
    return (long)GEN_JNI.org_chromium_base_metrics_RecordHistogram_recordLinearCountHistogram(name, key, sample, min, max, numBuckets);
  }

  @Override
  public long recordSparseHistogram(String name, long key, int sample) {
    return (long)GEN_JNI.org_chromium_base_metrics_RecordHistogram_recordSparseHistogram(name, key, sample);
  }

  @Override
  public int getHistogramValueCountForTesting(String name, int sample) {
    return (int)GEN_JNI.org_chromium_base_metrics_RecordHistogram_getHistogramValueCountForTesting(name, sample);
  }

  @Override
  public int getHistogramTotalCountForTesting(String name) {
    return (int)GEN_JNI.org_chromium_base_metrics_RecordHistogram_getHistogramTotalCountForTesting(name);
  }

  public static RecordHistogram.Natives get() {
    if (GEN_JNI.TESTING_ENABLED) {
      if (testInstance != null) {
        return testInstance;
      }
      if (GEN_JNI.REQUIRE_MOCK) {
        throw new UnsupportedOperationException("No mock found for the native implementation for org.chromium.base.metrics.RecordHistogram.Natives. The current configuration requires all native implementations to have a mock instance.");
      }
    }
    return new RecordHistogramJni();
  }
}
