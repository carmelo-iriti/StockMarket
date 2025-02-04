package com.ciriti.stockmarket.ui.stockprice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ciriti.stockmarket.data.RxSocketService
import com.ciriti.stockmarket.data.SubscribeCommand
import com.ciriti.stockmarket.data.UnSubscribeCommand
import com.ciriti.stockmarket.data.stockList
import com.ciriti.stockmarket.utils.Logger
import com.ciriti.stockmarket.utils.printThreadName
import com.tinder.scarlet.WebSocket
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class StockPriceViewModelRxJava(
    private val service: RxSocketService,
    private val errorHandler: (Throwable) -> Int,
    private val logger: Logger
) : ViewModel() {

    private val mutableLiveData by lazy { MutableLiveData<BaseState>() }
    val liveData: LiveData<BaseState> get() = mutableLiveData
    private val disposable = mutableListOf<CompositeDisposable>()

    fun subscribeAll() {

        disposable + service
            .observeWebSocketEvent()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { event ->
                    when (event) {
                        is WebSocket.Event.OnConnectionOpened<*> -> {
                            stockList.forEach { service.subscribe(SubscribeCommand(it)) }
                            getUpdate()
                        }
                        is WebSocket.Event.OnConnectionFailed -> {
                        }
                        is WebSocket.Event.OnMessageReceived -> {
                        }
                        is WebSocket.Event.OnConnectionClosed -> {
                        }
                        is WebSocket.Event.OnConnectionClosing -> {
                        }
                    }
                },
                { throwable ->
                    logger.e("${StockPriceViewModelRxJava::class.simpleName}", "", throwable)
                    /**
                     * process the exception type using the errorHandler fun
                     * and return a value to send the UI
                     */
                    mutableLiveData.postValue(BaseState.StateError(errorHandler(throwable)))
                }
            )
    }

    private fun getUpdate() {
        disposable + service
            .observeStock()
            .map { it.toUiModel() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { stockInfo ->
                    stockInfo.fold(
                        { throwable ->
                            logger.e(
                                "${StockPriceViewModelRxJava::class.simpleName}",
                                "",
                                throwable
                            )
                            /**
                             * process the exception type using the errorHandler fun
                             * and return a value to send the UI
                             */
                            mutableLiveData.postValue(BaseState.StateError(errorHandler(throwable)))
                        },
                        { ifRight ->
                            printThreadName("ViewModel obj $ifRight")
                            mutableLiveData.postValue(BaseState.StateSuccess(ifRight))
                        }
                    )
                },
                { throwable ->
                    logger.e("${StockPriceViewModelRxJava::class.simpleName}", "", throwable)
                    /**
                     * process the exception type using the errorHandler fun
                     * and return a value to send the UI
                     */
                    mutableLiveData.postValue(BaseState.StateError(errorHandler(throwable)))
                }
            )
    }

    fun unSubscribeAll() {
        stockList.forEach {
            service.unSubscribe(UnSubscribeCommand(it))
        }
    }

    override fun onCleared() {
        super.onCleared()
        disposable.map { it.dispose() }
    }
}
