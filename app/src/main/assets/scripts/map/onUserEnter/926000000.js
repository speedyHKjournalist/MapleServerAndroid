function start(ms) {
    var map = ms.getClient().getChannelServer().getMapFactory().getMap(926000000);
    map.resetPQ(1);

    if (map.countMonster(9100013) == 0) {
        map.spawnMonsterOnGroundBelow(LifeFactory.getMonster(9100013), new android.graphics.Point(82, 200));
    }

    return (true);
}
