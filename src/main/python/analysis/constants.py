import seaborn as sns
sns.set()
DRT_NAME = "DRT"

OCCUPANCY_COLS = ['4 pax',
                  '3 pax',
                  '2 pax',
                  '1 pax',
                  '0 pax',
                  'pickup',
                  'rebalance',
                  'stay',
                  'off-service'
                 ]

def get_color(data_name, color_set="deep"):
    if data_name == "stay":
        return sns.xkcd_rgb["greyish"]
    if data_name == "0 pax":
        return sns.xkcd_rgb["light olive"]
    palette = sns.color_palette(color_set, n_colors=len(OCCUPANCY_COLS))
    COL_TO_COLOR = dict(zip(OCCUPANCY_COLS, palette))
    return COL_TO_COLOR[data_name]

LONG_TO_SHORT_NAME = {
    "DRT": "DRT",
    "DynamicRideSharingStrategy": "DRSS",
    "ExtDemandSupplyBeamSharing": "Ext-DS",
    "HighCapacityDispatcher": "HCRS",
    "TShareDispatcher": "T-Share",
}