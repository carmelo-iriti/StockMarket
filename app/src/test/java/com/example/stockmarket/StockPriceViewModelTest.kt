package com.example.stockmarket

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.stockmarket.data.StockInfo
import com.example.stockmarket.ui.stockprice.BaseState
import com.example.stockmarket.ui.stockprice.StockPriceViewModel
import com.example.stockmarket.utils.Logger
import com.example.stockmarket.utils.debugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class StockPriceViewModelTest {

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    private val channel by lazy { Channel<StockInfo>() }

    @Before
    @ExperimentalCoroutinesApi
    fun setUp() { Dispatchers.setMain(Dispatchers.Unconfined) }

    @Test
    fun `send one update and receive a correct UI State`() = runBlocking<Unit> {

        val eventList = mutableListOf<BaseState>()

        val sut = StockPriceViewModel(
            service = StockServiceStub(channel, StockServiceStub.eventList),
            logger = Logger.debugLogger(),
            errorHandler = { R.string.error },
            pStockList = listOf("Apple"),
            workerDispatcher = Dispatchers.Unconfined
        )

        /** collect the events to the ui */
        sut.liveData.observeForever { eventList.add(it) }

        /** trigger the updates */
        sut.subscribeAll()

        /** send the update */
        channel.send(StockInfo("Apple", 10.001))

        /** check */
        eventList.size assertEquals 1
        (eventList[0] as? BaseState.StateSuccess)
            .assertNotNull()
            .uiStockModel.price assertEquals "10.00"

    }


    @Test
    fun `receive one connection error`() = runBlocking<Unit> {

        val eventList = mutableListOf<BaseState>()

        val sut = StockPriceViewModel(
            service = StockServiceStub(channel, StockServiceStub.errorEventList),
            logger = Logger.debugLogger(),
            errorHandler = { R.string.error },
            pStockList = listOf("Apple"),
            workerDispatcher = Dispatchers.Unconfined
        )

        /** collect the events to the ui */
        sut.liveData.observeForever { eventList.add(it) }

        /** trigger the updates */
        sut.subscribeAll()

        /** send the update */
        channel.send(StockInfo("Apple", 10.001))

        /** check */
        eventList.size assertEquals 2
        (eventList[0] as? BaseState.StateError)
            .assertNotNull()
            .errorMessage assertEquals R.string.error

    }

}

