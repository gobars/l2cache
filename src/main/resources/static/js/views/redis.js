(function ($) {

    var constant = {
        RESET_STATS_BUTTON: '.bind-reset-stats-button',
        SEARCH_BUTTON: '.bind-search-button',
        SEARCH_INPUT: '.bind-search-input',
        DELETE_PROMPT: '#delete-prompt',
        DELETE_CACHE_KEYINPUT: '#delete-cache-key-input',
        CONFIRM: '#my-confirm',
        DETAIL_MODAL: '#detail-modal',
    };

    var viewModel = {
        cacheStats: ko.observableArray([]),
        detailCacheStats: ko.observable({}),
        fcs: ko.observable({}),
        scs: ko.observable({}),
    };

    var bindEvent = {
        bindResetStats: function () {
            $(constant.RESET_STATS_BUTTON).on("click", function () {
                $(constant.CONFIRM).modal({
                    relatedTarget: this,
                    onConfirm: function (options) {
                        $.ajax({
                            type: 'POST',
                            url: 'cache-stats/reset-stats',
                            dataType: 'JSON',
                            success: function (data) {
                                bindEvent.getData();
                            }
                        });
                    },
                    // closeOnConfirm: false,
                    onCancel: function () {

                    }
                });
            });
        },
        searchData: function () {
            $(constant.SEARCH_BUTTON).on("click", function () {
                bindEvent.getData();
            });
        },
        getData: function () {

            var data = {
                "code": "200",
                "data": [{
                    "cacheName": "people1",
                    "desc": "描述",
                    "l1MissCount": 1,
                    "l1RequestCount": 2,
                    "hitRate": 50.0,
                    "internalKey": "4000-100000-3000",
                    "l2Setting": {
                        "desc": "描述",
                        "c1Setting": {
                            "allowNullValues": true,
                            "expireMode": "WRITE",
                            "expireSecs": 4,
                            "initCap": 10,
                            "maxSize": 5000,
                            "timeUnit": "SECONDS"
                        },
                        "internalKey": "4000-100000-3000",
                        "c2Setting": {
                            "allowNullValues": true,
                            "expireSecs": 100,
                            "forceRefresh": true,
                            "preloadSecs": 3,
                            "timeUnit": "SECONDS",
                            "usePrefix": true
                        },
                        "useFirstCache": true
                    },
                    "missCount": 1,
                    "requestCount": 2,
                    "l2MissCount": 1,
                    "l2RequestCount": 1,
                    "totalLoadTime": 52
                }],
                "message": "SUCCESS",
                "status": "SUCCESS"
            };
            var temp = ko.mapping.fromJS(data.data);
            format.formatInit(temp());
            viewModel.cacheStats(temp());

            // $.ajax({
            //     type: 'POST',
            //     url: 'cache-stats/list',
            //     dataType: 'JSON',
            //     data: {"cacheName": $(constant.SEARCH_INPUT).val()},
            //     success: function (data) {
            //         var temp = ko.mapping.fromJS(data.data);
            //         format.formatInit(temp());
            //         viewModel.cacheStats(temp());
            //     }
            // });
        }
    };

    var format = {
        formatInit: function (cacheStats) {
            $.each(cacheStats, function (i, cs) {
                cs.hitRate = cs.hitRate().toFixed(2);
                cs.l1HitRate = ((cs.l1RequestCount() - cs.l1MissCount()) / cs.l1RequestCount() * 100).toFixed(2);
                cs.l2HitRate = ((cs.l2RequestCount() - cs.l2MissCount()) / cs.l2RequestCount() * 100).toFixed(2);
                cs.avgTotalLoadTime = (cs.totalLoadTime() / cs.requestCount()).toFixed(2);

                cs.deleteCache = function () {
                    $(constant.DELETE_PROMPT).modal({
                        relatedTarget: this,
                        onConfirm: function (e) {
                            $(constant.CONFIRM).modal({
                                relatedTarget: this,
                                onConfirm: function (options) {
                                    $.ajax({
                                        type: 'POST',
                                        url: 'cache-stats/delete-cache',
                                        dataType: 'JSON',
                                        data: {
                                            "cacheName": cs.cacheName(),
                                            "internalKey": cs.internalKey(),
                                            "key": $(constant.DELETE_CACHE_KEYINPUT).val()
                                        },
                                        success: function (data) {
                                            $(constant.DELETE_CACHE_KEYINPUT).val("");
                                            bindEvent.getData();
                                        }
                                    });
                                },
                                // closeOnConfirm: false,
                                onCancel: function () {
                                    $(constant.DELETE_CACHE_KEYINPUT).val("");
                                }
                            });
                        },
                        onCancel: function (e) {
                            $(constant.DELETE_CACHE_KEYINPUT).val("");
                        }
                    });
                }

                cs.detail = function () {
                    viewModel.detailCacheStats(cs);
                    viewModel.fcs(cs.l2Setting.c1Setting);
                    viewModel.scs(cs.l2Setting.c2Setting);
                    $(constant.DETAIL_MODAL).modal({
                        relatedTarget: this,
                        width: 1000,
                        onConfirm: function (options) {

                        },
                        // closeOnConfirm: false,
                        onCancel: function () {

                        }
                    });
                }
            });
        }
    };

    var index = {
        init: function () {
            ko.applyBindings(viewModel);
            bindEvent.getData();
            bindEvent.searchData();
            bindEvent.bindResetStats();
        }
    };

    $(function () {
        index.init();
    });
})(jQuery);