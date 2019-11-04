package com.example.timmytruong.wordsearch_19.adapters

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.TableLayout
import android.widget.TextView
import com.example.timmytruong.wordsearch_19.interfaces.GridHandler
import com.example.timmytruong.wordsearch_19.interfaces.InformationBarHandler
import com.example.timmytruong.wordsearch_19.utils.DrawUtils
import com.example.timmytruong.wordsearch_19.utils.constant.AppConstants
import com.example.timmytruong.wordsearch_19.viewmodel.GridViewModel
import com.example.timmytruong.wordsearch_19.viewmodel.InformationBarViewModel
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class DrawAdapter(private val context: Context,
                  private val gridFrame: FrameLayout,
                  private val wordsTableLayout: TableLayout,
                  private val scoreTextView: TextView,
                  private val gridHandler: GridHandler,
                  private val gridViewModel: GridViewModel,
                  private val informationBarViewModel: InformationBarViewModel,
                  private val informationBarHandler: InformationBarHandler)
{

    private var initialPosition: Int = -1

    private var currentPosition: Int = -1

    private var allDirectionPossibilities: ArrayList<Int> = arrayListOf()

    private var directionPosition: Int = -1

    private var centreX: Int? = -1

    private var centreY: Int? = -1

    private var startCentreX: Int? = -1

    private var startCentreY: Int? = -1

    private var endViewNumber: Int = -1

    private var startViewNumber: Int = -1

    private val onTouchListener = View.OnTouchListener { v: View, event: MotionEvent ->

        val words: HashMap<String, Boolean> = gridViewModel.getWordsHashMap()

        val action: Int = event.actionMasked

        val grid: GridView = v as GridView

        val x: Int = event.x.toInt()

        val y: Int = event.y.toInt()

        val position: Int = grid.pointToPosition(x, y)

        val globalX: Int?

        val globalY: Int?

        if (position in 0..99)
        {
            val cellView: TextView = grid.findViewWithTag(position)

            val cellViewRect = Rect()

            cellView.getDrawingRect(cellViewRect)

            grid.offsetDescendantRectToMyCoords(cellView, cellViewRect)

            globalX = cellViewRect.left
            globalY = cellViewRect.top

            centreX = globalX + cellView.width / 2
            centreY = globalY + cellView.height / 2

            when (action)
            {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE ->
                {
                    if (currentPosition != position)
                    {
                        v.parent.requestDisallowInterceptTouchEvent(true)

                        when (action)
                        {
                            MotionEvent.ACTION_DOWN ->
                            {
                                initialPosition = position

                                allDirectionPossibilities = findAllDirectionPossibilities(initialPosition)

                                directionPosition = -1

                                startCentreX = globalX + cellView.width / 2

                                startCentreY = globalY + cellView.height / 2

                                drawLine(startCentreX, startCentreY, startCentreX, startCentreY, AppConstants.PAINT_COLOUR_YELLOW)

                                startViewNumber = gridFrame.childCount - 1

                                currentPosition = position
                            }
                            MotionEvent.ACTION_MOVE ->
                            {
                                if (allDirectionPossibilities.contains(position))
                                {
                                    gridHandler.removeSearchView(context, startViewNumber, gridFrame.childCount, gridFrame)

                                    directionPosition = directionFinder(position, initialPosition)

                                    drawLine(startCentreX, startCentreY, centreX, centreY, AppConstants.PAINT_COLOUR_YELLOW)

                                    currentPosition = position
                                }
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    Log.i("Start View Number", startViewNumber.toString() + "")
                    Log.i("Current View Number", gridFrame.childCount.toString() + "")
                    Log.i("Start Cell Number", initialPosition.toString() + "")
                    Log.i("End Cell Number", currentPosition.toString() + "")
                    if (startViewNumber != -1)
                    {
                        val formedWord = getWord(currentPosition, initialPosition)

                        endViewNumber = gridFrame.childCount

                        gridHandler.removeSearchView(context, startViewNumber, endViewNumber, gridFrame)

                        if (formedWord != null && words.contains(formedWord) && !(words[formedWord] as Boolean))
                        {
                            scoreHandler()

                            drawLine(startCentreX, startCentreY, centreX, centreY, AppConstants.PAINT_COLOUR_GREEN)

                            wordsHandler(words, formedWord)

                            winHandler()
                        }
                    }
                }
            }
        }
        else if (position == -1 && action == MotionEvent.ACTION_UP && startViewNumber != -1)
        {
            val formedWord = getWord(currentPosition, initialPosition)

            endViewNumber = gridFrame.childCount

            gridHandler.removeSearchView(context, startViewNumber, endViewNumber, gridFrame)

            if (formedWord != null && words.contains(formedWord) && !(words[formedWord] as Boolean))
            {
                scoreHandler()

                drawLine(startCentreX, startCentreY, centreX, centreY, AppConstants.PAINT_COLOUR_GREEN)

                wordsHandler(words, formedWord)

                winHandler()
            }
        }

        return@OnTouchListener true
    }

    fun getOnTouchListener(): View.OnTouchListener
    {
        return onTouchListener
    }

    private fun wordsHandler(words: HashMap<String, Boolean>, formedWord: String)
    {
        words[formedWord] = true

        gridViewModel.setWordsHashMap(words)

        gridHandler.strikeOutWord(context, formedWord, wordsTableLayout)
    }

    private fun scoreHandler()
    {
        informationBarViewModel.setScore(false)
        informationBarHandler.setScoreTextView(informationBarViewModel.getScore(), informationBarViewModel.getTotal(), scoreTextView)
    }

    private fun winHandler()
    {
        if (informationBarViewModel.getScore() == informationBarViewModel.getTotal())
        {
            gridHandler.displayWinDialogue(context)
        }
    }

    private fun drawLine(startX: Int?, startY: Int?, endX: Int?, endY: Int?, colour: Int)
    {
        val drawUtils = DrawUtils(context)

        gridFrame.addView(drawUtils)

        drawUtils.drawLine(startX!!.toFloat(), startY!!.toFloat(), endX!!.toFloat(), endY!!.toFloat(), colour)
    }

    private fun getWord(newPosition: Int, initialPosition: Int): String?
    {
        var directionInterval = 0
        var indexPosition = initialPosition
        var returnString = ""

        if (directionPosition != -1)
        {
            when (directionInterval)
            {
                AppConstants.DIRECTION_STRAIGHT_RIGHT -> directionInterval = 1
                AppConstants.DIRECTION_DIAGONAL_DOWN_RIGHT -> directionInterval = 11
                AppConstants.DIRECTION_STRAIGHT_DOWN -> directionInterval = 10
                AppConstants.DIRECTION_DIAGONAL_DOWN_LEFT -> directionInterval = 9
                AppConstants.DIRECTION_STRAIGHT_LEFT -> directionInterval = -1
                AppConstants.DIRECTION_DIAGONAL_UP_LEFT -> directionInterval = -11
                AppConstants.DIRECTION_STRAIGHT_UP -> directionInterval = -10
                AppConstants.DIRECTION_DIAGONAL_UP_RIGHT -> directionInterval = -9
            }

            val untilPosition = newPosition + directionInterval

            while (indexPosition != untilPosition)
            {
                returnString += gridViewModel.getLettersHashMap()[indexPosition]
                indexPosition += directionInterval
            }

            return returnString
        }
        return null
    }

    private fun directionFinder(newPosition: Int, initialPosition: Int): Int
    {
        val directionHint = newPosition - initialPosition

        if (isCellLower(directionHint))
        {
            when {
                isVertical(directionHint) -> return AppConstants.DIRECTION_STRAIGHT_UP
                isHorizontal(directionHint) -> return AppConstants.DIRECTION_STRAIGHT_LEFT
                isLeftHighDiagonal(directionHint) -> return AppConstants.DIRECTION_DIAGONAL_UP_LEFT
                isLeftLowDiagonal(directionHint) -> return AppConstants.DIRECTION_DIAGONAL_UP_RIGHT
            }
        }
        else
        {
            when {
                isVertical(directionHint) -> return AppConstants.DIRECTION_STRAIGHT_DOWN
                isHorizontal(directionHint) -> return AppConstants.DIRECTION_STRAIGHT_RIGHT
                isLeftHighDiagonal(directionHint) -> return AppConstants.DIRECTION_DIAGONAL_DOWN_RIGHT
                isLeftLowDiagonal(directionHint) -> return AppConstants.DIRECTION_DIAGONAL_DOWN_LEFT
            }
        }
        return -1
    }

    private fun isCellLower(directionHint: Int): Boolean
    {
        return directionHint < 0
    }

    private fun isVertical(directionHint: Int): Boolean
    {
        return abs(directionHint) % 10 == 0
    }

    private fun isLeftHighDiagonal(directionHint: Int): Boolean
    {
        return abs(directionHint) % 11 == 0
    }

    private fun isLeftLowDiagonal(directionHint: Int): Boolean
    {
        return abs(directionHint) % 9 == 0
    }

    private fun isHorizontal(directionHint: Int): Boolean
    {
        return abs(directionHint) <= 9
    }

    private fun findAllDirectionPossibilities(position: Int): ArrayList<Int>
    {
        val savedPosition: Int = position
        var newPosition = position
        val possiblePositions: ArrayList<Int> = arrayListOf()

        while (newPosition <= ((position / 10) * 10) + 9)
        {
            possiblePositions.add(newPosition)
            newPosition += 1
        }

        newPosition = savedPosition

        while (newPosition <= 99)
        {
            if (!possiblePositions.contains(newPosition))
            {
                possiblePositions.add(newPosition)
            }

            if (newPosition % 10 == 9)
            {
                break
            }

            newPosition += 11
        }

        newPosition = savedPosition

        while (newPosition <= 99)
        {
            if (!possiblePositions.contains(newPosition))
            {
                possiblePositions.add(newPosition)
            }
            newPosition += 10
        }

        newPosition = savedPosition

        while (newPosition <= 99)
        {
            if (!possiblePositions.contains(newPosition))
            {
                possiblePositions.add(newPosition)
            }

            if (newPosition % 10 == 0)
            {
                break
            }

            newPosition += 9
        }

        newPosition = savedPosition

        while (newPosition >= ((position / 10) * 10))
        {
            if (!possiblePositions.contains(newPosition))
            {
                possiblePositions.add(newPosition)
            }
            newPosition -= 1
        }

        newPosition = savedPosition

        while (newPosition >= 0)
        {
            if (!possiblePositions.contains(newPosition))
            {
                possiblePositions.add(newPosition)
            }

            if (newPosition % 10 == 0)
            {
                break
            }

            newPosition -= 11
        }

        newPosition = savedPosition

        while (newPosition >= 0)
        {
            if (!possiblePositions.contains(newPosition))
            {
                possiblePositions.add(newPosition)
            }
            newPosition -= 10
        }

        newPosition = savedPosition

        while (newPosition >= 0)
        {
            if (!possiblePositions.contains(newPosition))
            {
                possiblePositions.add(newPosition)
            }

            if (newPosition % 10 == 9)
            {
                break
            }

            newPosition -= 9
        }

        return possiblePositions
    }
}