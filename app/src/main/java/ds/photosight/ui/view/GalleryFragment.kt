package ds.photosight.ui.view

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import ds.photosight.R
import ds.photosight.utils.action
import ds.photosight.utils.snack
import ds.photosight.utils.toggle
import ds.photosight.ui.adapter.GalleryAdapter
import ds.photosight.ui.adapter.MenuAdapter
import ds.photosight.ui.adapter.MenuPagerAdapter
import ds.photosight.ui.widget.HideableBottomSheet
import ds.photosight.ui.viewmodel.GalleryViewModel
import ds.photosight.ui.viewmodel.MainViewModel
import ds.photosight.ui.viewmodel.MenuItemState
import ds.photosight.ui.viewmodel.MenuState.Companion.MENU_CATEGORIES
import ds.photosight.ui.viewmodel.MenuState.Companion.MENU_RATINGS
import ds.photosight.utils.position
import kotlinx.android.synthetic.main.fragment_gallery.*
import kotlinx.android.synthetic.main.view_menu.*
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class GalleryFragment : Fragment() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    @Inject
    lateinit var log: Timber.Tree

    private val viewModel: GalleryViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    private val transitionHelper = SharedElementsHelper(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_gallery, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().window.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.translucent)
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.translucent)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.position
            ?.let { position ->
                log.v("observed new position: $position")
                transitionHelper.postpone(position)
            }

        setupMenu()
        setupAppBar()
        fixInsets()
        observeData()


    }

    private fun observeData() {
        val onMenuSelected = { item: MenuItemState ->

            viewModel.onMenuSelected(item)
        }
        val categoriesAdapter = MenuAdapter(onMenuSelected)
        val ratingsAdapter = MenuAdapter(onMenuSelected)
        menuViewPager.adapter = MenuPagerAdapter(categoriesAdapter, ratingsAdapter)
        setupTabs()

        // hide bottom sheet until categories loaded
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        val photosAdapter = GalleryAdapter(transitionHelper) { clickedItem ->
            log.v("clicked on ${clickedItem.view.transitionName} pos=${clickedItem.position}")
            if (clickedItem.isLoading) {
                viewModel.loadingState.value = true
            } else {
                val extras = FragmentNavigatorExtras(
                    clickedItem.view to clickedItem.view.transitionName
                )
                with(findNavController()) {
                    log.v("current destination: ${currentDestination?.id}")
                    if (R.id.galleryFragment == currentDestination?.id) {
                        navigate(GalleryFragmentDirections.openViewer(clickedItem.position), extras)
                    } else {
                        log.w("wrong destination")
                    }
                }
            }
        }

        photosRecyclerView.adapter = photosAdapter

        mainViewModel.setMenuStateLiveData(viewModel.menuStateLiveData)

        viewModel.menuStateLiveData.observe(viewLifecycleOwner) {
            log.v("menu state observed")
            categoriesAdapter.updateData(it.categories)
            ratingsAdapter.updateData(it.ratings)
            toolbar.title = it.getSelected().title
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            if (bottomSheetBehavior.isHideable) {
                Handler().post {
                    bottomSheetBehavior.isHideable = false
                }
                photosAdapter.addLoadStateListener { state ->
                    log.v("loading state: $state")
                    viewModel.loadingState.value = state.refresh is LoadState.Loading || state.append is LoadState.Loading || state.prepend is LoadState.Loading
                    if (state.refresh is LoadState.Error || state.append is LoadState.Error || state.prepend is LoadState.Error) {
                        viewModel.onLoadingError()
                    }
                }
            }

        }

        mainViewModel.photosPagedLiveData.observe(viewLifecycleOwner) { photos ->
            log.v("photos list observed")
            photosAdapter.submitData(lifecycle, photos)
            transitionHelper.moveToCurrentItem(photosRecyclerView)

        }
        viewModel.loadingState.observe(viewLifecycleOwner) { isLoading ->
            log.v("isloading observed: $isLoading")
            progress.toggle(isLoading)
        }
        viewModel.retrySnackbarCommand.observe(viewLifecycleOwner) { message ->
            showRetrySnackbar(message) {
                photosAdapter.retry()
            }
        }

    }

    private fun setupAppBar() {
        // fixes returning image transition glitch
        appBar.setExpanded(true, false)

        val topPadding = toolbar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val sbInset = insets.systemWindowInsetTop
            view.updatePadding(top = topPadding + sbInset)
            insets
        }
    }

    private fun fixInsets() {
        val initialPeekHeight = resources.getDimension(R.dimen.peek_height).toInt()
        var sbInset = 0
        var nbInset = 0

        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { view, insets ->
            sbInset = insets.systemWindowInsetTop
            nbInset = insets.systemWindowInsetBottom
            bottomSheetBehavior.setPeekHeight(initialPeekHeight + nbInset, true)
            tabLayout.updatePadding(
                top = 0,
                bottom = nbInset
            )
            insets
        }
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                tabLayout.updatePadding(
                    top = (sbInset * slideOffset).toInt(),
                    bottom = (nbInset * (1 - slideOffset)).toInt()
                )
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                log.v("onStateChanged $newState")
            }
        })

        (bottomSheetBehavior as HideableBottomSheet).stateCallback = { state ->
            // snack bar placeholder setup
            val lp = snackbarLayout.layoutParams as CoordinatorLayout.LayoutParams
            if (state == HideableBottomSheet.State.VISIBLE) {
                lp.anchorId = bottomSheet.id
            } else {
                lp.anchorId = screenBottom.id
            }
            snackbarLayout.layoutParams = lp
        }
    }

    private fun setupMenu() {
        toolbar.inflateMenu(R.menu.menu_main)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.im_about -> requireActivity().showAbout()
                else -> error("not implemented")
            }
            true
        }
    }

    private fun setupTabs() {
        //tabLayout.isClickable=true
        /*bottomSheet.setOnClickListener {
            openMenu()
        }*/
        /*tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                openMenu()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                openMenu()
            }

        })*/

        TabLayoutMediator(tabLayout, menuViewPager) { tab, position ->
            tab.view.setOnClickListener {
                openMenu()
            }
            tab.text = when (position) {
                MENU_CATEGORIES -> getString(R.string.categories)
                MENU_RATINGS -> getString(R.string.ratings)
                else -> error("wrong tab")
            }
        }.attach()
    }

    private fun openMenu() {
        log.v("open menu")
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun showRetrySnackbar(message: String, callback: (View) -> Unit) {
        snackbarLayout.snack(message, Snackbar.LENGTH_INDEFINITE) {
            anchorView = snackbarLayout
            action(R.string.retry, R.color.accent, callback)
        }
    }

}

