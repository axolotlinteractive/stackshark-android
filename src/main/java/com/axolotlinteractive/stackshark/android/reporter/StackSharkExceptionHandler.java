package com.axolotlinteractive.stackshark.android.reporter;

/**
 * Created by JakeW on 11/2/14.
 */

class StackSharkExceptionHandler implements Thread.UncaughtExceptionHandler
{
    private Thread.UncaughtExceptionHandler handle;
    StackSharkExceptionHandler()
    {
        handle = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex)
    {
        ErrorReporter.handleCaughtException(ex);
        handle.uncaughtException(thread, ex);
    }
}