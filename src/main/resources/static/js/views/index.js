(function ($) {

    var constant = {
        RESET_STATS_BUTTON: '.bind-reset-stats-button',
        SEARCH_BUTTON: '.bind-search-button',
        REDIS_CONFIG_FORM: '#redis-config-form',
        REDIS_CONFIG_BUTTON: '#redis-config-button',
        REDIS_SELECT: '#bind-redis-select option:selected',
        SEARCH_INPUT: '.bind-search-input',
        DELETE_PROMPT: '#delete-prompt',
        DELETE_CACHE_KEYINPUT: '#delete-cache-key-input',
        RESET_CONFIRM: '#reset-confirm',
        DELETE_CONFIRM: '#delete-confirm',
        DETAIL_MODAL: '#detail-modal',
        LOGIN_OUT: '#login-out',
    };

    var viewModel = {
        cacheStats: ko.observableArray([]),
        redisList: ko.observableArray([]),
        detailCacheStats: ko.observable({}),
        fcs: ko.observable({}),
        scs: ko.observable({}),
    };

    var token = {
        getToken: function () {
            var url = location.search;
            if (url.indexOf("?") !== -1) {
                var str = url.substr(1);
                return str.split("=")[1];
            }
        }
    };

    var bindEvent = {
        bindResetStats: function () {
            $(constant.RESET_STATS_BUTTON).on("click", function () {
                $(constant.RESET_CONFIRM).modal({
                    relatedTarget: this,
                    onConfirm: function (options) {
                        $.ajax({
                            type: 'POST',
                            url: '/cache-stats/reset-stats',
                            dataType: 'JSON',
                            data: {"redisClient": $(constant.REDIS_SELECT).val(), "token": token.getToken()},
                            success: function (data) {
                                if (data.status === "SUCCESS") {
                                    bindEvent.getData();
                                } else {
                                    alert(data.message);
                                }
                            },
                            error: function () {
                                window.location.href = "index.html";
                            }
                        });
                    },
                    // closeOnConfirm: false,
                    onCancel: function () {

                    }
                });
            });
        },
        bindLoginOut: function () {
            $(constant.LOGIN_OUT).on("click", function () {
                $.ajax({
                    type: 'POST',
                    url: '/user/login-out',
                    dataType: 'JSON',
                    data: {"token": token.getToken()},
                    success: function (data) {
                        if (data.status === "SUCCESS") {
                            window.location.href = "/toLogin";
                        } else {
                            alert("操作失败");
                        }
                    },
                    error: function () {
                        window.location.href = "/toLogin";
                    }
                });
            });
        },
        searchData: function () {
            $(constant.SEARCH_BUTTON).on("click", function () {
                bindEvent.getData();
            });
        },
        redisConfig: function () {
            $(constant.REDIS_CONFIG_BUTTON).on("click", function () {
                $.ajax({
                    type: 'POST',
                    url: '/redis/redis-config',
                    dataType: 'JSON',
                    data: $(constant.REDIS_CONFIG_FORM).serialize(),
                    success: function (data) {
                        if (data.status === "SUCCESS") {
                            bindEvent.redisList();
                        } else {
                            alert(data.message);
                        }
                    }
                });
            });
        },
        redisList: function () {
            $.ajax({
                type: 'POST',
                url: '/redis/redis-list',
                dataType: 'JSON',
                data: {"token": token.getToken()},
                success: function (data) {
                    if (data.status === "SUCCESS") {
                        viewModel.redisList(data.data);
                    } else {
                        alert(data.message);
                    }
                }
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
                        "desc": "描述啦",
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

            $.ajax({
                type: 'POST',
                url: '/cache-stats/list',
                dataType: 'JSON',
                data: {
                    "redisClient": $(constant.REDIS_SELECT).val(),
                    "cacheName": $(constant.SEARCH_INPUT).val(),
                    "token": token.getToken()
                },
                success: function (data) {
                    var temp = ko.mapping.fromJS(data.data);
                    format.formatInit(temp());
                    viewModel.cacheStats(temp());
                },
                error: function () {
                    window.location.href = "/toLogin";
                }
            });
        }
    };

    var format = {
        formatInit: function (cacheStats) {
            $.each(cacheStats, function (i, cs) {
                let hitRate = cs.hitRate();
                if (hitRate && !isNaN(hitRate)) {
                    cs.hitRate = hitRate;
                } else {
                    cs.hitRate = 0;
                }
                cs.hitRate = parseFloat(cs.hitRate.toFixed(2));

                let l1RequestCount = cs.l1RequestCount();
                if (l1RequestCount > 0) {
                    cs.l1HitRate = (l1RequestCount - cs.l1MissCount()) / l1RequestCount;
                } else {
                    cs.l1HitRate = 0
                }
                cs.l1HitRate = parseFloat((cs.l1HitRate * 100).toFixed(2));

                let l2RequestCount = cs.l2RequestCount();
                if (l2RequestCount > 0) {
                    cs.l2HitRate = (l2RequestCount - cs.l2MissCount()) / l2RequestCount;
                } else {
                    cs.l2HitRate = 0;
                }
                cs.l2HitRate = parseFloat((cs.l2HitRate * 100).toFixed(2));


                if (cs.missCount() > 0) {
                    cs.avgTotalLoadTime = cs.totalLoadTime() / cs.missCount();
                } else {
                    cs.avgTotalLoadTime = 0;
                }
                cs.avgTotalLoadTime = parseFloat(cs.avgTotalLoadTime.toFixed(2));

                cs.deleteCache = function () {
                    $(constant.DELETE_PROMPT).modal({
                        relatedTarget: this,
                        onConfirm: function (e) {
                            $(constant.DELETE_CONFIRM).modal({
                                relatedTarget: this,
                                onConfirm: function (options) {
                                    $.ajax({
                                        type: 'POST',
                                        url: '/cache-stats/delete-cache',
                                        dataType: 'JSON',
                                        data: {
                                            "cacheName": cs.cacheName(),
                                            "internalKey": cs.internalKey(),
                                            "key": $(constant.DELETE_CACHE_KEYINPUT).val(),
                                            "redisClient": $(constant.REDIS_SELECT).val(),
                                            "token": token.getToken()
                                        },
                                        success: function (data) {
                                            if (data.status === "SUCCESS") {
                                                $(constant.DELETE_CACHE_KEYINPUT).val("");
                                                bindEvent.getData();
                                            } else {
                                                alert(data.message);
                                            }
                                        },
                                        error: function () {
                                            window.location.href = "index.html";
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
            bindEvent.searchData();
            bindEvent.redisConfig();
            bindEvent.bindResetStats();
            bindEvent.bindLoginOut();
        }
    };

    $(function () {
        index.init();
    });
})(jQuery);