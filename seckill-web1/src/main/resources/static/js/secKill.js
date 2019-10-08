//js函数式开发
// function f1() {
//
// }
// function f2() {
//
// }
//2.js的模块化编程 (封装的思想) ---》比作为对象方便理解
var secKillModule ={
    //属性
    contextPath:"",
    //定时器返回的id
    timeFlag:"",
    //字段
    url:{
        randomURL: function () {
            return secKillModule.contextPath+"/seckill/random/";
        },
        seckillURL:function(){
            return secKillModule.contextPath +"/seckill/gooods/";
        },
        //查询秒杀结果的接口
        querySecKillResultURL:function () {
            return secKillModule.contextPath+"/seckill/querySecKillResult/"
        }
    },

    //方法
    func :{
        initDetail:function (productId,currentTime,startTime,endTime) {

            //当前时间<开始时间			 距离秒杀还有xxxx
            if(currentTime<startTime){
                //秒杀还没有开始
                //使用jquery的倒计时插件实现倒计时
                /* + 1000 防止时间偏移 这个没有太多意义，因为我们并不知道客户端和服务器的时间偏移
                这个插件简单了解，实际项目不会以客户端时间作为倒计时的，所以我们在服务器端还需要验证*/
                var killTime = new Date(startTime + 1000);
                $("#secKillTip").countdown(killTime, function (event) {
                    //时间格式
                    var format = event.strftime('距秒杀开始还有: %D天 %H时 %M分 %S秒');
                    $("#secKillTip").html("<span style='color:red;'>"+format+"</span>");
                }).on('finish.countdown', function () {
                    //倒计时结束后回调事件，已经开始秒杀，用户可以进行秒杀了，有两种方式：
                    //1、刷新当前页面
                    location.reload();
                    //或者2、调用秒杀开始的函数


                });

            }else if(currentTime>endTime){
                //当前时间>结束时间			秒杀结束
                $("#secKillTip").html("<span style='color: red;'>爽了吧 让你早点来你不来 秒杀结束啦</span>");

            }else {
                //开始时间<当前时间<结束时间	秒杀开始       开始秒杀的按钮可以供用户点击
                //$("#secKillTip").html("<button type='button' id='secKillBut'>立即秒杀</button>");
                secKillModule.func.prepareSecKill(productId)
            }
        },
        //秒杀开始前的准备
        prepareSecKill:function (productId) {
            //1.在后台验证秒杀是否真的开始  防止程序员跳过页面的验证，直接通过脚本代码去秒杀
           // 2.随机数--返回一个唯一的标识码给前端 才能进行点击秒杀 --秒杀接口的暴露
           $.ajax({
               url:secKillModule.url.randomURL()+productId,
               type:"post",
               dataType:"json",
               success:function (ret2FrontObjet) {
                   //本次是OK
                   if(ret2FrontObjet.errorCode==1){
                       var frontRandom=ret2FrontObjet.data;
                       if(frontRandom){
                           //显示立即秒杀的按钮

                           $("#secKillTip").html("<button type='button' id='secKillBut'>立即秒杀</button>");
                           $("#secKillBut").click(function () {
                               //真正执行秒杀的函数

                                   secKillModule.func.executeSecKill(frontRandom,productId);
                           })
                       }
                   }else {
                       $("#secKillTip").html("<span style='color: red;'>"+ret2FrontObjet.errorMessage+"</span>");
                   }
               }

           });

        },
        executeSecKill:function (frontRandom,productId) {

            $.ajax({
                //url格式：   /15-seckill-web/seckill/gooods/Ffdaskfjkadlsjklfa/1
                url: secKillModule.url.seckillURL() + frontRandom +"/" +productId,
                type:"post",
                dataType:"json",
                success:function (rtnMessage) {

                    //处理响应结果
                    if(rtnMessage.errorCode == 1){
                        //秒杀成功，已经下单到MQ，返回中间结果  可以做动画处理
                        $("#secKillTip").html("<span style='color:red;'>"+ rtnMessage.errorMessage +"</span>");
                        //接下来再发送一个请求获取最终秒杀的结果,,因为使用了消息队列,所以需要定时器一直询问,知道结果传回前端
                        secKillModule.timeFlag=window.setInterval(function () {
                            secKillModule.func.querySecKillResult(productId);
                        },3*1000);

                    }else{
                        $("#secKillTip").html("<span style='color:red;'>"+ rtnMessage.errorMessage +"</span>");
                    }
                }
            });

        },
        querySecKillResult:function (productId) {
            $.ajax({
                url: secKillModule.url.querySecKillResultURL() + productId,
                type: "post",
                dataType: "json",
                success: function (ret2FrontObjet) {
                    if (  ret2FrontObjet.errorCode==1) {
                        $("#secKillTip").html("<span style='color: blue;'>"+ret2FrontObjet.errorMessage+"<a href='http://www.alipay.com' target='_blank'>去支付</a></span>");
                        //当查询到秒杀成功的信息之后 关闭定时器 浪费系统性能
                        window.clearInterval(secKillModule.intervalName);
                    }else if(ret2FrontObjet.errorCode==0){
                        //秒杀
                        $("#secKillTip").html("<span style='color: blue;'>"+ret2FrontObjet.errorMessage+"</span>");
                        //当查询到秒杀成功的信息之后 关闭定时器 浪费系统性能
                        window.clearInterval(secKillModule.intervalName);
                    }

                }
            });

        }

    }
}