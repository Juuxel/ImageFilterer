package juuxel.imagefilterer

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Creates Java `Executor`s and runs and cancels them.
 * Useful for simulating Kotlin coroutines without experimental features.
 */
class TaskExecutor
{
    private val executors = HashMap<Any, ExecutorService?>()

    /**
     * Starts a concurrent task and associates it with the [id].
     */
    fun addTask(id: Any, task: () -> Unit)
    {
        val executor = Executors.newSingleThreadExecutor()
        executor.submit(task)
        executors[id] = executor
    }

    /**
     * Stops the task with the specified [id].
     */
    fun stopTask(id: Any)
    {
        executors[id]?.let {
            it.shutdownNow()
            it.awaitTermination(1000L, TimeUnit.MILLISECONDS)
            executors[id] = null
        }
    }
}
