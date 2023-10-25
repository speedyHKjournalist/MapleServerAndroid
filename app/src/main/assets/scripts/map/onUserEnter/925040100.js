function start(ms) {
    var player = ms.getPlayer();
    var map = player.getMap();

    if (ms.isQuestStarted(21747) && ms.getQuestProgressInt(21747, 9300351) == 0) {
        importPackage(Packages.server.life);
        importClass(android.graphics.Point);
        map.spawnMonsterOnGroundBelow(LifeFactory.getMonster(9300351), new Point(897, 51));
    }
}